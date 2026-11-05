"""
P8 bloque 1: el ícono de cada app, generado desde un solo dibujo.

Todas las apps comparten los edificios del logo original; cambia el símbolo de arriba a la
derecha (el visto bueno en la de residentes) y el color de fondo. Escribe, por app:
  - values/ic_launcher_background.xml   color de fondo del ícono y de la pantalla de inicio
  - drawable/ic_launcher_foreground.xml símbolo blanco del ícono adaptativo (y monocromo)
  - drawable/logo_animado.xml           el mismo símbolo dibujado trazo a trazo al abrir la app
  - mipmap-*/ic_launcher(.png|_round.png) íconos para Android 7 (sin ícono adaptativo)

Uso (desde la raíz del repo; necesita Pillow):  python tools/iconos.py
"""
import math
import os
import sys

from PIL import Image, ImageDraw

RAIZ = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "app", "src")

# Edificios del logo original (caja de 108 del ícono adaptativo)
VENTANAS = [(49, 36), (56, 36), (49, 44), (56, 44), (49, 52), (56, 52), (37.5, 53), (37.5, 61), (67.5, 59), (67.5, 66)]
LADO_VENTANA = 3.6
TRAZOS = {  # nombre: (puntos, grosor)
    "suelo": ([(31, 76), (77, 76)], 3.2),
    "edificio_izq": ([(34, 76), (34, 47), (45, 47)], 3.2),
    "torre": ([(45, 76), (45, 31), (54.5, 25), (64, 31), (64, 76)], 3.2),
    "edificio_der": ([(64, 76), (64, 53), (75, 53), (75, 76)], 3.2),
}
PUERTA = ([(51.5, 76), (51.5, 67), (57.5, 67), (57.5, 76)], 2.6)


def circulo(cx, cy, r):
    return ("circulo", (cx, cy, r))


def linea(*puntos):
    return ("linea", puntos)


def engranaje(cx=74.6, cy=39.5):
    # Separado de la torre (x = 64) para que no se toquen
    partes = [circulo(cx, cy, 3.3)]
    for k in range(6):
        a = math.radians(60 * k + 30)
        partes.append(linea((cx + 4.6 * math.cos(a), cy + 4.6 * math.sin(a)),
                            (cx + 6.2 * math.cos(a), cy + 6.2 * math.sin(a))))
    return partes


def cuadricula():
    partes = []
    for x in (67.8, 74.6):
        for y in (32.8, 39.6):
            partes.append(linea((x, y), (x + 4.6, y), (x + 4.6, y + 4.6), (x, y + 4.6), (x, y)))
    return partes


# Por app: carpeta del flavor, color de fondo, símbolo (partes) y grosor del símbolo
APPS = {
    "main": ("#5E35B1", [linea((65, 41), (70, 46), (78, 37))], 3.6),            # residentes: visto bueno
    "administracion": ("#283593", engranaje(), 2.6),                            # engranaje
    "recepcion": ("#E65100", [circulo(70.4, 39.5, 2.9), linea((73.3, 39.5), (81.6, 39.5)),
                              linea((78.8, 39.5), (78.8, 42.7)), linea((81.6, 39.5), (81.6, 42.1))], 2.6),  # llave
    "plataforma": ("#00796B", cuadricula(), 2.2),                               # cuadrícula
}

NOMBRES_APP = {"main": "residentes", "administracion": "administración", "recepcion": "portería",
               "plataforma": "plataforma"}


def f(v):
    return ("%.2f" % v).rstrip("0").rstrip(".")


def datos_trazo(puntos):
    return "M" + f(puntos[0][0]) + "," + f(puntos[0][1]) + " " + " ".join(
        "L" + f(x) + "," + f(y) for x, y in puntos[1:])


def datos_simbolo(partes):
    salida = []
    for tipo, d in partes:
        if tipo == "circulo":
            cx, cy, r = d
            salida.append("M%s,%s a%s,%s 0 1,0 %s,0 a%s,%s 0 1,0 -%s,0" % (
                f(cx - r), f(cy), f(r), f(r), f(2 * r), f(r), f(r), f(2 * r)))
        else:
            salida.append(datos_trazo(d))
    return " ".join(salida)


def datos_ventanas():
    l = f(LADO_VENTANA)
    return " ".join("M%s,%sh%sv%sh-%sz" % (f(x), f(y), l, l, l) for x, y in VENTANAS)


def trazo_xml(sangria, datos, grosor, extra="", nombre=None):
    s = sangria
    attrs = []
    if nombre:
        attrs.append('android:name="%s"' % nombre)
    attrs.append('android:pathData="%s"' % datos)
    attrs.append('android:strokeWidth="%s"' % f(grosor))
    attrs.append('android:strokeColor="#FFFFFFFF"')
    attrs.append('android:strokeLineCap="round"')
    attrs.append('android:strokeLineJoin="round"')
    if extra:
        attrs.append(extra)
    return s + "<path\n" + "\n".join(s + "    " + a for a in attrs) + " />\n"


def frente(app):
    _, partes, grosor = APPS[app]
    x = ['<?xml version="1.0" encoding="utf-8"?>\n',
         "<!-- Generado por tools/iconos.py (no editar a mano). Símbolo del logo de la app de %s:\n" % NOMBRES_APP[app],
         "     los edificios del logo original y su símbolo, blanco sobre el fondo del ícono. Caja de\n",
         "     108 dp reducida al 88 % para quedar dentro de la zona segura; también es el monocromo. -->\n",
         '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n',
         '    android:width="108dp"\n    android:height="108dp"\n',
         '    android:viewportWidth="108"\n    android:viewportHeight="108">\n',
         '    <group\n        android:pivotX="54"\n        android:pivotY="50.5"\n',
         '        android:scaleX="0.88"\n        android:scaleY="0.88"\n        android:translateY="3.5">\n',
         '        <path\n            android:fillColor="#FFFFFFFF"\n            android:pathData="%s" />\n' % datos_ventanas()]
    for puntos, g in TRAZOS.values():
        x.append(trazo_xml("        ", datos_trazo(puntos), g))
    x.append(trazo_xml("        ", datos_simbolo(partes), grosor))
    x.append('        <path\n            android:pathData="%s"\n            android:strokeWidth="%s"\n'
             '            android:strokeColor="#FFFFFFFF"\n            android:strokeLineJoin="round" />\n'
             % (datos_trazo(PUERTA[0]), f(PUERTA[1])))
    x.append("    </group>\n</vector>\n")
    return "".join(x)


def animador(nombre, propiedad, duracion, inicio, desde="0", hasta="1", interpolador="fast_out_slow_in"):
    return ('    <target android:name="%s">\n        <aapt:attr name="android:animation">\n            <set>\n'
            '                <objectAnimator\n                    android:duration="%d"\n'
            '                    android:interpolator="@android:interpolator/%s"\n'
            '                    android:propertyName="%s"\n                    android:startOffset="%d"\n'
            '                    android:valueFrom="%s"\n                    android:valueTo="%s"\n'
            '                    android:valueType="floatType" />\n            </set>\n        </aapt:attr>\n    </target>\n'
            % (nombre, duracion, interpolador, propiedad, inicio, desde, hasta))


def animado(app):
    _, partes, grosor = APPS[app]
    s = "                "
    x = ['<?xml version="1.0" encoding="utf-8"?>\n',
         "<!-- Generado por tools/iconos.py (no editar a mano). Logo animado de la carga inicial de la app\n",
         "     de %s: el símbolo del ícono se dibuja trazo a trazo, se encienden las ventanas y termina\n" % NOMBRES_APP[app],
         "     con el símbolo de la app. Empieza vacío: sin start() no se ve nada. -->\n",
         '<animated-vector xmlns:android="http://schemas.android.com/apk/res/android"\n',
         '    xmlns:aapt="http://schemas.android.com/aapt">\n',
         '    <aapt:attr name="android:drawable">\n        <vector\n            android:width="108dp"\n',
         '            android:height="108dp"\n            android:viewportWidth="108"\n            android:viewportHeight="108">\n',
         '            <group\n                android:name="logo"\n                android:pivotX="54"\n',
         '                android:pivotY="50.5"\n                android:scaleX="0.88"\n                android:scaleY="0.88"\n',
         '                android:translateY="3.5">\n',
         s + '<path\n' + s + '    android:name="ventanas"\n' + s + '    android:fillAlpha="0"\n' + s
         + '    android:fillColor="#FFFFFFFF"\n' + s + '    android:pathData="%s" />\n' % datos_ventanas()]
    for nombre, (puntos, g) in TRAZOS.items():
        x.append(trazo_xml(s, datos_trazo(puntos), g, 'android:trimPathEnd="0"', nombre))
    x.append(trazo_xml(s, datos_simbolo(partes), grosor, 'android:trimPathEnd="0"', "visto"))
    x.append(s + '<path\n' + s + '    android:name="puerta"\n' + s + '    android:pathData="%s"\n' % datos_trazo(PUERTA[0])
             + s + '    android:strokeAlpha="0"\n' + s + '    android:strokeWidth="%s"\n' % f(PUERTA[1])
             + s + '    android:strokeColor="#FFFFFFFF"\n' + s + '    android:strokeLineJoin="round" />\n')
    x.append("            </group>\n        </vector>\n    </aapt:attr>\n")
    x.append(animador("suelo", "trimPathEnd", 260, 0))
    x.append(animador("edificio_izq", "trimPathEnd", 300, 120))
    x.append(animador("torre", "trimPathEnd", 380, 200))
    x.append(animador("edificio_der", "trimPathEnd", 300, 320))
    # El símbolo de la app tiene más trazos que el visto bueno: se dibuja un poco más despacio
    x.append(animador("visto", "trimPathEnd", 260 if app == "main" else 380, 620))
    x.append(animador("puerta", "strokeAlpha", 200, 520))
    x.append(animador("ventanas", "fillAlpha", 260, 560))
    x.append('    <target android:name="logo">\n        <aapt:attr name="android:animation">\n            <set>\n')
    for eje in ("scaleX", "scaleY"):
        x.append('                <objectAnimator\n                    android:duration="900"\n'
                 '                    android:interpolator="@android:interpolator/overshoot"\n'
                 '                    android:propertyName="%s"\n                    android:startOffset="0"\n'
                 '                    android:valueFrom="0.792"\n                    android:valueTo="0.88"\n'
                 '                    android:valueType="floatType" />\n' % eje)
    x.append("            </set>\n        </aapt:attr>\n    </target>\n</animated-vector>\n")
    return "".join(x)


def fondo(app):
    color = APPS[app][0]
    return ('<?xml version="1.0" encoding="utf-8"?>\n<resources>\n'
            "    <!-- Generado por tools/iconos.py: fondo del ícono y de la pantalla de inicio (app de %s) -->\n"
            '    <color name="ic_launcher_background">%s</color>\n</resources>\n' % (NOMBRES_APP[app], color))


# ---- PNG para Android 7: se dibuja la caja de 108 y se recorta el centro (72), como el sistema
ESCALA = 8


def png(app, tam, redondo):
    color, partes, grosor = APPS[app]
    lado = 108 * ESCALA
    img = Image.new("RGBA", (lado, lado), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)

    def p(x, y):  # grupo: escala 0.88 alrededor de (54, 50.5) y baja 3.5
        return ((54 + (x - 54) * 0.88) * ESCALA, (50.5 + (y - 50.5) * 0.88 + 3.5) * ESCALA)

    def ancho(g):
        return g * 0.88 * ESCALA

    def poli(puntos, g, redondear=True):
        w = ancho(g)
        pts = [p(*q) for q in puntos]
        d.line(pts, fill="white", width=int(round(w)), joint="curve")
        if redondear:
            for q in (pts[0], pts[-1]):
                d.ellipse([q[0] - w / 2, q[1] - w / 2, q[0] + w / 2, q[1] + w / 2], fill="white")

    def anillo(cx, cy, r, g):
        c = p(cx, cy)
        rr = r * 0.88 * ESCALA
        w = ancho(g)
        d.ellipse([c[0] - rr - w / 2, c[1] - rr - w / 2, c[0] + rr + w / 2, c[1] + rr + w / 2], outline="white",
                  width=int(round(w)))

    # fondo: el ícono visible es el centro de 72 de la caja de 108
    m = 18 * ESCALA
    fondo_rgb = tuple(int(color[i:i + 2], 16) for i in (1, 3, 5))
    if redondo:
        d.ellipse([m, m, lado - m, lado - m], fill=fondo_rgb)
    else:
        d.rounded_rectangle([m, m, lado - m, lado - m], radius=12 * ESCALA, fill=fondo_rgb)
    for x, y in VENTANAS:
        a, b = p(x, y), p(x + LADO_VENTANA, y + LADO_VENTANA)
        d.rectangle([a[0], a[1], b[0], b[1]], fill="white")
    for puntos, g in TRAZOS.values():
        poli(puntos, g)
    poli(PUERTA[0], PUERTA[1], redondear=False)
    for tipo, dd in partes:
        if tipo == "circulo":
            anillo(*dd, grosor)
        else:
            poli(dd, grosor)
    recorte = img.crop((m, m, lado - m, lado - m))
    return recorte.resize((tam, tam), Image.LANCZOS)


DENSIDADES = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}


def escribir(ruta, texto):
    os.makedirs(os.path.dirname(ruta), exist_ok=True)
    with open(ruta, "w", encoding="utf-8", newline="\n") as a:
        a.write(texto)


def main():
    apps = sys.argv[1:] or list(APPS)
    for app in apps:
        res = os.path.join(RAIZ, app, "res")
        escribir(os.path.join(res, "values", "ic_launcher_background.xml"), fondo(app))
        escribir(os.path.join(res, "drawable", "ic_launcher_foreground.xml"), frente(app))
        escribir(os.path.join(res, "drawable", "logo_animado.xml"), animado(app))
        for densidad, tam in DENSIDADES.items():
            carpeta = os.path.join(res, "mipmap-" + densidad)
            os.makedirs(carpeta, exist_ok=True)
            png(app, tam, False).save(os.path.join(carpeta, "ic_launcher.png"))
            png(app, tam, True).save(os.path.join(carpeta, "ic_launcher_round.png"))
        print("listo:", app)


if __name__ == "__main__":
    main()
