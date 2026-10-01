# Taller 3 — Mecanismos de Persistencia en Android

Universidad de Nariño · Ingeniería de Sistemas · Desarrollo de Aplicaciones Móviles

**Integrantes:**

David Sebastián Barahona Chamorro
Manuel David Jesús Rosero Vera
Jesús Ricardo Villota Iguad
Brayan Hernán Perenguez Pérez


---

## Descripción general

El proyecto es una app de **sesiones de enfoque (Pomodoro)** llamada **App A**, acompañada de una segunda app independiente, **App B**, que consume datos de App A. Entre las dos cubren los 5 mecanismos de persistencia vistos en el taller.

| Punto | Mecanismo | Dónde vive |
| --- | --- | --- |
| 1 | Estado del ciclo de vida (Bundle) | App A |
| 2 | Almacenamiento local permanente (Room) + Offline-First | App A |
| 3 | Compartición de datos entre apps (ContentProvider) | App A expone, App B consume |
| 4 | Seguridad y cifrado | App A |
| 5 | Persistencia en la nube (API + caché híbrida) | App A |

## Estructura del repositorio

```
.
├── app-a-pomodoro/     Proyecto de Android Studio — App A (Pomodoro)
├── app-b-resumen/      Proyecto de Android Studio — App B (Resumen)
└── README.md
```

Cada carpeta se abre como un proyecto independiente en Android Studio. **App A debe instalarse siempre antes que App B**, porque es quien declara el permiso personalizado que B necesita.

---

## App A — Sesiones de Enfoque (Pomodoro)

`applicationId`: `com.example.persistencia`

### Punto 1: Estado del ciclo de vida (temporal)

La pantalla de sesión (`TareaScreen`) tiene un cronómetro y un campo de notas, y **no usa ViewModel**, según lo exige el taller.

- **`onSaveInstanceState` / `onRestoreInstanceState`:** guardan y recuperan en un `Bundle` el texto de las notas, la posición del cursor, si el campo tenía el foco, los segundos del cronómetro, la meta en minutos y si el cronómetro estaba corriendo.
- **`onPause()` / `onStop()`:** guardan un borrador rápido del texto en `SharedPreferences` (no en el Bundle, porque `onStop()` no lo recibe), para recuperarlo si el proceso muere en segundo plano.
- **Comportamiento del cronómetro:**
  - Al **rotar la pantalla**, el conteo continúa sin interrupción perceptible.
  - Al pasar a **segundo plano**, el conteo se pausa y se reanuda al volver, solo si el usuario lo tenía en marcha.
  - Si el usuario **cierra la app manualmente** (deslizarla fuera de recientes), el proceso se termina por decisión del usuario y Android no restaura el Bundle; el cronómetro vuelve a cero. Esto es el comportamiento esperado: el Bundle es para destrucción controlada por el sistema (rotación, poca memoria, "No conservar actividades"), no para el cierre manual.

**Qué se guarda en estado de instancia y qué no (respuesta a la pregunta teórica del punto 1):**

- *Sí:* datos pequeños de interfaz, temporales y fáciles de recrear (texto en edición, cursor, foco, contador, banderas, pantalla actual).
- *No:* datos que deben sobrevivir al cierre de la app o que son grandes (las tareas, el tiempo acumulado total). Eso vive en Room.

### Punto 2: Almacenamiento local permanente (Room) + Offline-First

- **Entidad `Tarea`:** id, título, descripción, estado completado, fecha de creación, tiempo acumulado, más dos campos para Offline-First: `pendienteSincronizacion` y `eliminada` (borrado lógico).
- **CRUD completo** vía `TareaDao` y Room: crear, listar, actualizar (completar / guardar sesión) y eliminar.
- **Offline-First:**
  - Cada cambio (crear, completar, eliminar) se marca como `pendienteSincronizacion = true`.
  - Un `ConnectivityManager.NetworkCallback` detecta cuándo hay o no conexión.
  - Al recuperar la red, `sincronizarPendientes()` recorre los registros pendientes y los "sube" (simulado con un retraso); los eliminados lógicamente se borran de verdad al confirmarse la sincronización.
  - La pantalla principal muestra un banner con el estado (sin conexión, pendientes, sincronizando, todo sincronizado).
- **Migraciones reales:** la base de datos pasó por las versiones 1 a 4, todas con `Migration` explícitas que conservan los datos existentes (nunca se usó `fallbackToDestructiveMigration`).

### Punto 3: Compartición de datos entre aplicaciones (ContentProvider)

- `TareaContentProvider` expone dos URIs protegidas por el permiso personalizado `com.example.persistencia.permission.READ_TAREAS`:
  - `content://com.example.persistencia.provider/tareas`: devuelve título, descripción, estado y tiempo de cada tarea (sin las eliminadas lógicamente).
  - `content://com.example.persistencia.provider/notasConfidenciales`: devuelve solo la **cantidad** de notas confidenciales guardadas, nunca su contenido (ver Punto 4).
- El permiso es de nivel `normal`, por lo que se concede automáticamente al instalar App B, sin necesidad de login ni aprobación manual del usuario.
- Sin el permiso declarado en su manifiesto, App B recibe una `SecurityException` al intentar leer.

### Punto 4: Seguridad en el almacenamiento y cifrado

Pantalla "Notas Confidenciales", protegida por una **contraseña maestra** que el propio usuario crea la primera vez que entra.

- La contraseña se guarda como **hash SHA-256**, nunca en texto plano.
- Las notas (título + contenido) se guardan en una **bóveda cifrada** con `EncryptedSharedPreferences` (AES-256).
- Cada vez que se guarda la bóveda, se escribe además una **copia sin cifrar** en almacenamiento externo (`getExternalFilesDir()`), solo con fines de comparación.
- La propia pantalla permite ver esa copia externa en texto plano, para contrastar visualmente qué tan legible queda frente a la copia cifrada.

**Evidencia de la comparación (ítem 4 del taller):**

| Almacenamiento | Ubicación | Resultado al inspeccionar |
| --- | --- | --- |
| Interno, cifrado | `shared_prefs/notas_seguras_prefs.xml` | Contenido **ilegible**, cifrado en Base64/binario |
| Externo, sin cifrar | `Android/data/com.example.persistencia/files/notas_confidenciales_sin_cifrar.txt` | Contenido **legible en texto plano**, títulos y notas completos |

Verificado con Device File Explorer de Android Studio y con:

```
adb shell cat /storage/emulated/0/Android/data/com.example.persistencia/files/notas_confidenciales_sin_cifrar.txt
```

**Conclusión:** cualquier app o persona con acceso al sistema de archivos del dispositivo (por ejemplo, retirando la tarjeta SD o con acceso root) puede leer la copia externa sin ningún esfuerzo, mientras que la copia interna permanece protegida incluso si el archivo se extrae, porque la clave de cifrado vive en el Android Keystore y no viaja con el archivo.

### Punto 5: Persistencia deslocalizada en la nube

- **API pública:** [JSONPlaceholder](https://jsonplaceholder.typicode.com/) (`GET /todos?_limit=20`), sin autenticación.
- **Caché híbrida:**
  1. Al abrir el catálogo, se muestran de inmediato las tareas sugeridas ya guardadas en Room (tabla `tabla_tareas_sugeridas`), sin esperar a la red.
  2. En segundo plano se descarga la lista actualizada con Retrofit y se guarda localmente.
  3. Si no hay conexión, la pantalla sigue mostrando el caché anterior y lo indica con un aviso.
- **Importación:** cada sugerencia tiene un botón "Importar", que la convierte en una tarea real de la app (tabla `Tarea`). A partir de ahí es una tarea normal: aparece en el listado principal y se le puede aplicar el cronómetro de enfoque como a cualquier otra.

---

## App B — Resumen de Enfoque

`applicationId`: `com.example.app_b_resumen`

App de solo lectura que consume datos de App A a través de su `ContentProvider`.

- Declara `<uses-permission android:name="com.example.persistencia.permission.READ_TAREAS" />`.
- Al tocar "Consultar tareas de App A":
  - Lee la URI de tareas y muestra título, descripción, estado y total de tiempo enfocado.
  - Lee la URI de notas confidenciales y muestra solo la mención "🔒 Hay N notas confidenciales guardadas en App A", sin revelar su contenido.
- Si App A no está instalada, o si el permiso no está declarado, muestra el error correspondiente en pantalla en vez de fallar silenciosamente.

---

## Cómo ejecutar el proyecto

1. Abre `app-a-pomodoro/` en Android Studio y ejecútalo primero en el emulador o dispositivo.
2. Abre `app-b-resumen/` en otra ventana de Android Studio y ejecútalo sobre el mismo emulador/dispositivo.
3. Si vienes de una versión anterior de la base de datos (antes del Punto 5), desinstala App A antes de correrla de nuevo, para que las migraciones se apliquen desde una base limpia.

## Cómo probar cada punto

**Punto 1:**

- Inicia el cronómetro, escribe notas, gira la pantalla varias veces: el tiempo, el texto, el cursor y el foco se conservan.
- Activa "No conservar actividades" en Opciones de Desarrollador, cambia de app y vuelve: todo se restaura.

**Punto 2:**

- Crea, completa y elimina tareas. Cierra la app, reinicia el emulador, ábrela de nuevo: los datos persisten.
- Activa el modo avión, haz cambios, desactívalo: la app sincroniza sola y el banner lo refleja.

**Punto 3:**

- Con App A instalada, abre App B y consulta. Desinstala el permiso de App B (comentando la línea del manifiesto) para ver el fallo por `SecurityException`.

**Punto 4:**

- Entra a Notas Confidenciales, crea la contraseña, agrega notas.
- Inspecciona con Device File Explorer o ADB los dos archivos mencionados arriba.

**Punto 5:**

- Abre el catálogo con conexión: debe actualizar y mostrar 20 sugerencias.
- Importa una y verifica que aparece como tarea normal en la pantalla principal.
- Con modo avión activado, confirma que se sigue mostrando el caché ya descargado.

---

## Tecnologías usadas

- Kotlin + Jetpack Compose
- Room (SQLite)
- Navigation Compose
- Retrofit + Gson + OkHttp
- Jetpack Security (`EncryptedSharedPreferences`)
- ContentProvider / ContentResolver

## Control de versiones

El historial de commits está organizado por punto del taller y por archivo, con mensajes del tipo `feat:`, `fix:` o `refactor:` según corresponda. Puede revisarse directamente en el historial de Git de cada carpeta (`app-a-pomodoro/` y `app-b-resumen/`) dentro de este mismo repositorio.