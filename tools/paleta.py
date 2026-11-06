"""Genera los roles M3 (claro y oscuro) de cada app a partir de su color, con el mismo esquema
TonalSpot que dio la paleta morada original. Escribe res/values(-night)/colors_app.xml por flavor."""
import os, sys
# Necesita: pip install materialyoucolor   Uso: python tools/paleta.py [app ...]
from materialyoucolor.hct import Hct
from materialyoucolor.scheme.scheme_tonal_spot import SchemeTonalSpot
from materialyoucolor.dynamiccolor.material_dynamic_colors import MaterialDynamicColors as M

RAIZ = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'app', 'src')
APPS = {'administracion': ('#283593', 'Admin'), 'recepcion': ('#E65100', 'Portería'), 'plataforma': ('#00796B', 'Plataforma'),
        }
ROLES = ['primary', 'on_primary', 'primary_container', 'on_primary_container', 'secondary', 'on_secondary',
         'secondary_container', 'on_secondary_container', 'tertiary', 'on_tertiary', 'tertiary_container',
         'on_tertiary_container', 'surface', 'on_surface', 'surface_variant', 'on_surface_variant',
         'surface_container_lowest', 'surface_container_low', 'surface_container', 'surface_container_high',
         'surface_container_highest', 'outline', 'outline_variant', 'inverse_surface', 'inverse_on_surface',
         'inverse_primary']
ATTR = {'primary': 'primary', 'on_primary': 'onPrimary', 'primary_container': 'primaryContainer',
        'on_primary_container': 'onPrimaryContainer', 'secondary': 'secondary', 'on_secondary': 'onSecondary',
        'secondary_container': 'secondaryContainer', 'on_secondary_container': 'onSecondaryContainer',
        'tertiary': 'tertiary', 'on_tertiary': 'onTertiary', 'tertiary_container': 'tertiaryContainer',
        'on_tertiary_container': 'onTertiaryContainer', 'surface': 'surface', 'on_surface': 'onSurface',
        'surface_variant': 'surfaceVariant', 'on_surface_variant': 'onSurfaceVariant',
        'surface_container_lowest': 'surfaceContainerLowest', 'surface_container_low': 'surfaceContainerLow',
        'surface_container': 'surfaceContainer', 'surface_container_high': 'surfaceContainerHigh',
        'surface_container_highest': 'surfaceContainerHighest', 'outline': 'outline', 'outline_variant': 'outlineVariant',
        'inverse_surface': 'inverseSurface', 'inverse_on_surface': 'inverseOnSurface', 'inverse_primary': 'inversePrimary'}


def hexa(argb):
    return '#%06X' % (argb & 0xFFFFFF)


def esquema(semilla, oscuro):
    s = SchemeTonalSpot(Hct.from_int(int('FF' + semilla[1:], 16)), oscuro, 0.0)
    return {r: hexa(getattr(M, ATTR[r]).get_argb(s)) for r in ROLES}


def con_alfa(alfa, color):
    return '#%s%s' % (alfa, color[1:])


for app, (semilla, nombre) in APPS.items():
    if len(sys.argv) > 1 and app not in sys.argv[1:]:
        continue
    for oscuro in (False, True):
        c = esquema(semilla, oscuro)
        # Como en la paleta morada original: en modo claro el color principal es el de la marca
        if oscuro:
            c['inverse_primary'] = semilla
        else:
            c['primary'] = semilla
        lineas = ['<?xml version="1.0" encoding="utf-8"?>', '<resources>',
                  '    <!-- Generado desde %s (app de %s), esquema TonalSpot de Material 3, modo %s.' % (semilla, nombre, 'oscuro' if oscuro else 'claro'),
                  '         Sobrescribe la paleta de main/res para que toda la interfaz use el color de esta app. -->']
        lineas += ['    <color name="md_%s">%s</color>' % (r, c[r]) for r in ROLES]
        sup, cont = c['surface'], c['surface_container']
        lineas += [
            '    <!-- Encabezados, botones flotantes y difuminados con el color de la app -->',
            '    <color name="colorPrimary">%s</color>' % semilla,
            '    <color name="boton_flotante">%s</color>' % con_alfa('B3', semilla),
            '    <color name="barra_subtitulo">%s</color>' % esquema(semilla, False)['primary_container'],
            '    <color name="aviso_general">%s</color>' % semilla,
            '    <color name="velo_desenfoque">%s</color>' % con_alfa('66', sup),
            '    <color name="velo_barra">%s</color>' % con_alfa('99', cont),
            '    <color name="barra_translucida">%s</color>' % con_alfa('B3', cont),
            '    <color name="fondo_barra_respaldo">%s</color>' % con_alfa('F2', cont),
            '    <color name="franja_inicio">%s</color>' % con_alfa('F2', sup),
            '    <color name="franja_fin">%s</color>' % con_alfa('00', sup),
        ]
        if oscuro:
            lineas.append('    <color name="white">%s</color>' % c['inverse_on_surface'])
        lineas.append('</resources>')
        carpeta = os.path.join(RAIZ, app, 'res', 'values-night' if oscuro else 'values')
        os.makedirs(carpeta, exist_ok=True)
        with open(os.path.join(carpeta, 'colors_app.xml'), 'w', encoding='utf-8', newline='\n') as f:
            f.write('\n'.join(lineas) + '\n')
        print(app, 'oscuro' if oscuro else 'claro', c['primary'], c['surface'], c['primary_container'])
