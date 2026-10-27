# A futuro: web de recepción, web de administración y apps de iPhone

Hoy hay dos apps Android del mismo código (P5):

| App | Paquete | Compilar |
|---|---|---|
| MiConjunto (residentes) | `com.example.residencial` | `./gradlew assembleResidenteDebug` |
| MiConjunto Admin (administración y contador) | `com.example.residencial.admin` | `./gradlew assembleAdministracionDebug` |

Todo lo que hacen pasa por la API REST del backend (Conjunto-Backend), así que las próximas
plataformas no necesitan otro backend:

- **Web de recepción** (portería): visitantes frecuentes, citófonos, parqueaderos de
  invitados y anuncios. Puede ser una web sencilla (React, Vue o Thymeleaf en el mismo
  backend) que inicia sesión con una cuenta de rol nuevo `RECEPCION`.
- **Web de administración**: las mismas pantallas de la app Admin. La API ya devuelve todo en
  JSON y acepta el mismo token (`Authorization: Bearer ...`).
- **Apps de iPhone** (residente y administración): SwiftUI nativo, o Kotlin Multiplatform /
  Compose Multiplatform para compartir la lógica de `api/`, `model/` y `util/`.
  Las notificaciones push ya usan Firebase, que también funciona en iOS (falta registrar las
  apps iOS en el proyecto `miconjunto-70216` y subir la clave APNs).

Para empezar cualquiera de ellas:

1. Activar la documentación de la API con `SWAGGER_ENABLED=true` y abrir `/swagger-ui.html`.
2. Agregar el dominio de la web a `CORS_ORIGINS` en Render.
3. Reutilizar el login: `POST /api/auth/admin/login` (administración) y el de residentes.
