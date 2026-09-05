# Macro Automator

App para grabar toques y textos en pantalla y reproducirlos después,
de forma inmediata o programada a cierta hora. Usa el
`AccessibilityService` nativo de Android — el mismo mecanismo que
usan los lectores de pantalla — así que no necesita root.

La interfaz (lo que ves y tocas dentro de la app) está hecha en
HTML/CSS/JS puro, dentro de `www/`. Lo único nativo (Kotlin) es el
puente que habla con el sistema operativo para pedir el permiso,
grabar gestos reales y reproducirlos.

## ⚠️ Importante antes de usarla

- Esto **no es un producto terminado de Play Store**: es la base
  funcional de tu experimento. Vas a necesitar compilarlo tú mismo
  y probablemente ajustar cosas la primera vez (nombres de paquete,
  íconos, permisos de Android 13+/14 que piden confirmación extra
  para accesibilidad).
- El permiso de accesibilidad le da a la app la capacidad de leer
  la pantalla y tocar cosas por ti. Actívalo solo en tu propio
  teléfono, y solo si confías en el código (que es exactamente el
  que está en este proyecto).
- Grabar clicks reales dentro de WhatsApp funciona bien para botones
  normales (abrir chat, botón de enviar). Para el campo de texto del
  mensaje, en vez de "simular que escribes letra por letra", el
  servicio escribe el texto completo directo en el campo enfocado
  (`ACTION_SET_TEXT`) — es más confiable.

## Opción A — Compilar 100% desde el celular (sin PC)

Este proyecto ya incluye `.github/workflows/build-apk.yml`, que hace
que GitHub compile el APK por ti en la nube. Tú solo subes el código
desde el celular.

**Pasos:**

1. Crea una cuenta gratuita en [github.com](https://github.com) si no tienes.
2. Crea un repositorio nuevo (puede ser privado), por ejemplo `macro-automator`.
3. Sube el contenido de esta carpeta al repositorio. Formas de hacerlo desde el celular:
   - **Más fácil:** instala la app **Termux** desde F-Droid (no está en Play Store), y dentro corre:
     ```bash
     pkg install git
     cd macro-automator
     git init
     git remote add origin https://github.com/TU_USUARIO/macro-automator.git
     git add .
     git commit -m "primera version"
     git branch -M main
     git push -u origin main
     ```
     (te va a pedir tu usuario y un "personal access token" de GitHub en vez de contraseña — se genera en Settings → Developer settings → Personal access tokens).
   - **Alternativa sin Termux:** en la web de GitHub (desde el navegador del celular) puedes arrastrar/subir los archivos manualmente con el botón "Add file → Upload files", carpeta por carpeta.
4. En tu repositorio, entra a la pestaña **Actions**. Vas a ver que el workflow "Build APK" corre solo.
5. Cuando termine (icono verde ✔, toma unos minutos), entra a esa ejecución y baja hasta **Artifacts** → descarga `macro-automator-apk`. Es un `.zip` que contiene el `app-debug.apk`.
6. Descomprímelo en tu celular (con cualquier app de archivos) e instala el APK directo (Android te pedirá permitir "instalar apps de fuentes desconocidas" la primera vez).

Todo esto — subir código, ver el progreso, descargar el resultado — se puede hacer desde el navegador del celular sin instalar nada más que Termux (y eso solo para el `git push`).

## Opción B — Compilar con Android Studio en una PC

### Requisitos

- Node.js 18+
- Android Studio (con el SDK de Android instalado)
- JDK 17

### Pasos para generar el APK

```bash
# 1. Entra a la carpeta del proyecto
cd macro-automator

# 2. Instala las dependencias de Capacitor
npm install

# 3. Genera el esqueleto completo del proyecto Android
npx cap add android

# 4. Copia nuestros archivos nativos personalizados encima del esqueleto
rm -f android/app/src/main/java/com/dany/macroautomator/MainActivity.java
cp -rf android-overlay/. android/

# 5. Sincroniza el código web (www/) hacia el proyecto Android
npx cap sync android

# 5. Abre el proyecto en Android Studio
npx cap open android
```

Dentro de Android Studio:

1. Espera a que termine el "Gradle Sync" (primera vez tarda varios minutos).
2. Conecta tu celular por USB con "Depuración USB" activada, o usa un emulador.
3. Presiona el botón ▶ (Run) para instalar y correr la app directamente.
4. Para generar el `.apk` instalable: menú **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
5. El archivo queda en `android/app/build/outputs/apk/debug/app-debug.apk`.

## Cómo usarla

1. Abre la app → botón **"Activar accesibilidad"** → te manda a los
   ajustes de Android → busca "Macro Automator" en la lista → actívala.
2. Regresa a la app, presiona **Grabar**.
3. Sal de la app (botón home), abre WhatsApp normalmente y haz los
   pasos que quieres automatizar (abrir el chat, tocar el campo de
   texto, pegar/escribir el mensaje, tocar enviar).
4. Vuelve a Macro Automator y presiona **Detener**.
5. Verás la lista de pasos grabados. Ponle un nombre y presiona
   **Guardar**.
6. Desde la lista de secuencias guardadas puedes:
   - **Reproducir**: la ejecuta ya mismo.
   - **Programar**: elige hora y (opcional) días de la semana para
     que se dispare sola, aunque la app esté cerrada.

## Estructura del proyecto

```
macro-automator/
├── www/                        → Interfaz (HTML/CSS/JS)
│   ├── index.html
│   ├── style.css
│   └── app.js
├── android-overlay/            → Nuestros archivos nativos (se inyectan
│   └── app/src/main/             sobre el esqueleto que genera Capacitor)
│       ├── AndroidManifest.xml
│       ├── res/xml/accessibility_service_config.xml
│       ├── res/values/strings.xml
│       └── java/com/dany/macroautomator/
│           ├── MainActivity.kt
│           ├── MacroPlugin.kt
│           ├── MacroAccessibilityService.kt
│           └── MacroAlarmReceiver.kt
├── .github/workflows/build-apk.yml  → Compila el APK en la nube
├── package.json
└── capacitor.config.json

Nota: la carpeta `android/` con el proyecto completo (gradlew,
build.gradle, etc.) NO está en el repositorio — la genera
automáticamente `npx cap add android` cada vez que se compila, y
luego el workflow copia encima los archivos de `android-overlay/`.
```

## Limitaciones conocidas (para no llevarte sorpresas)

- Si WhatsApp cambia el diseño de sus botones, una secuencia grabada
  con coordenadas fijas podría dejar de acertar el toque exacto —
  es normal en cualquier automatización basada en coordenadas.
- Algunos fabricantes (Xiaomi, Huawei, Samsung con "optimización de
  batería" agresiva) pueden matar el servicio en segundo plano. Si
  las secuencias programadas dejan de dispararse, hay que
  desactivar la optimización de batería para esta app específica.
- Desde Android 13, después de instalar el APK manualmente, el
  sistema pide una confirmación extra ("acceso restringido") antes
  de dejarte activar el servicio de accesibilidad — es un paso
  adicional, no un error del código.
