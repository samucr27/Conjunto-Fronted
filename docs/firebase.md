# Avisos en el celular (Firebase Cloud Messaging)

La app ya trae todo lo necesario para que los avisos (nueva reserva, nuevo vehículo…) lleguen
al panel de notificaciones del celular y, al tocarlos, abran la pantalla correspondiente.
Solo falta conectar un proyecto de Firebase, que es gratis. Sin él, la app y el backend
funcionan igual, pero no llegan avisos.

Hay que hacerlo una sola vez y toma unos 15 minutos.

## 1. Crear el proyecto de Firebase

1. Entra a <https://console.firebase.google.com> con tu cuenta de Google.
2. **Crear un proyecto** → nombre: `MiConjunto`. Google Analytics no hace falta (puedes desactivarlo).

## 2. Registrar la app Android → `google-services.json`

1. En el proyecto: **Agregar app → Android**.
2. Nombre del paquete: **`com.example.residencial`** (tiene que ser exactamente ese).
   Desde P5 hay dos apps: registra también **`com.example.residencial.admin`** (app de
   administración). El `google-services.json` descargado trae las dos.
3. Apodo: `MiConjunto` (opcional). La huella SHA-1 no hace falta.
4. Descarga **`google-services.json`** y cópialo en la carpeta **`app/`** del proyecto
   (junto a `app/build.gradle.kts`).
5. Los pasos de "Agregar el SDK" que muestra Firebase ya están hechos: puedes saltarlos.

Al volver a compilar, Gradle detecta el archivo y activa Firebase solo.

> ¿Subirlo a GitHub? Es seguro: no es una contraseña, solo identifica la app ante Firebase.
> Si lo subes, tus compañeros compilan con avisos sin hacer nada; si no, cada uno debe copiarlo.

## 3. Clave del servidor → variable `FIREBASE_CREDENTIALS` en Render

El backend necesita una clave para enviar los avisos.

1. En Firebase: ⚙️ **Configuración del proyecto → Cuentas de servicio**.
2. **Generar nueva clave privada** → se descarga un archivo `.json`.
3. En Render, en el servicio del backend: **Environment → Add Environment Variable**
   - Key: `FIREBASE_CREDENTIALS`
   - Value: pega **todo el contenido** del archivo `.json` (desde `{` hasta `}`).
4. Guarda: Render reinicia el backend. En sus logs debe aparecer
   `Avisos push activos (Firebase Cloud Messaging)`.

> ⚠️ Esta clave **sí es secreta**: no la subas a GitHub ni la compartas. Si se filtra, en esa
> misma pantalla de Firebase puedes borrarla y generar otra.

## 4. Probar

1. Instala la app desde Android Studio e inicia sesión como administrador. En Android 13 o
   superior, acepta el permiso de notificaciones.
2. Cierra la app. Desde otro celular (o el emulador) inicia sesión como residente y crea una
   reserva o registra un vehículo.
3. Al administrador le llega el aviso. Al tocarlo se abre la app (pide iniciar sesión si hace
   falta) y lleva a la reserva o al apartamento del vehículo.

## Cómo funciona (para el equipo)

- **App**: `push/ServicioMensajes.kt` recibe el aviso y `push/AvisosCelular.kt` lo muestra en el
  canal "Avisos". Al iniciar sesión el celular se registra en el backend (`POST /api/dispositivos`)
  y al cerrarla se da de baja (`POST /api/dispositivos/baja`). Al tocar el aviso,
  `MainActivity` abre la pantalla con `ui/comun/DestinoNotificacion.kt`.
- **Backend**: al guardar una notificación, `push/AvisosPush.java` la envía en segundo plano a
  los celulares del administrador y borra los que Firebase ya no acepta.
