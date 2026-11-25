# Smarter Hoppers — Plan

Mod de Fabric para Minecraft Java 26.2 (solo servidor) que sustituye a *Easy Multi Item Sorter* y enruta los ítems dentro de las torres de almacenamiento sin atascos.

## Objetivos
1. Cada ítem va al cofre que **ya lo contiene**; la tolva siguiente es el último recurso.
2. Ningún ítem entra en una torre si no tiene **sitio reservable** en ella; el sobrante sigue por la línea hasta el overflow.

## Montaje de cada torre
```
L  (tolva de la línea → siguiente tolva de la línea)
M  (contenedor marcado, debajo de L)       [item frame con comparador en un lateral]
H0 (tolva nueva debajo de M → apunta a T1)   T1 (antigua tolva superior, bajo el item frame)
                                             T1 → cofre 4, T2 debajo → cofre 3 ... → cofre 1
```
- **Contenedor marcado:** cualquier contenedor (barril, cofre, shulker…) con un item frame (normal o luminoso; como entidad vanilla o como bloque de Fast Item Frames) en cualquier cara con un comparador dentro. En un cofre doble basta con que una mitad esté marcada.
- **Salidas de una tolva:** el contenedor al que apunta, más una tolva vanilla justo debajo, más un contenedor marcado justo debajo.
  - **Una salida:** comportamiento vanilla puro, sin cálculos (H0, la última tolva de cada torre y las tolvas de línea sin filtro).
  - **Dos salidas:** enrutado inteligente (L, T1–T3).

## Reglas
| # | Regla |
|---|---|
| R1 | Una tolva solo saca X de un contenedor marcado si dentro hay **más de 2 X en total** |
| R2 | No se mete X en un contenedor marcado si X no está ya dentro |
| R3 | Si un contenedor marcado está debajo de una tolva de la línea, esa tolva le mete X con prioridad, siempre que R2 y R4 lo permitan; si no, empuja X a la siguiente |
| R4 | Solo entra X en el contenedor marcado si la torre de debajo tiene **sitio reservable** para X |
| T1 | Una tolva de torre con dos salidas solo empuja X a su cofre si el cofre contiene X y tiene hueco; si no, X queda para la tolva de abajo |
| T2 | La tolva de abajo solo succiona lo que la de arriba ha decidido no meter en su cofre |
| T3 | Anticipación: una tolva con dos salidas no deja pasar X hacia abajo si ningún cofre por debajo puede guardarlo |

## Reservas (R4)
Algoritmo de **simulación**:
1. Tomar una "foto" del espacio libre de los cofres de la torre.
2. Insertar virtualmente lo que ya está en tránsito (M, H0, T1–T4), en el orden en que lo haría el enrutado.
3. Comprobar si X todavía cabe.

- Los huecos vacíos de un cofre son un **fondo común** para todos los tipos que guarda.
- `margen_cofre`: dejar de aceptar ítems cuando a un cofre le queden menos de N huecos libres.
- **Coste estimado:** unas 270 lecturas de huecos por decisión (de 1 a 5 µs). Con 80 torres son menos de 0,05 ms por tick.
- Empezar recalculando la simulación cada vez y medir con spark. Solo si hace falta, pasar a contadores incrementales.
- Se guarda en caché la **forma de cada torre** (tolvas, cofres y marcas) y se invalida cuando cambia un bloque o un item frame de esa torre.

### Garantía y límite
- No hay atascos porque: todo ítem entra con un sitio reservado, el flujo solo baja y no tiene ciclos, y una tolva vanilla prueba todos sus huecos al empujar.
- **Límite:** si se insertan ítems a mano en un cofre de la torre (o lo hace otro mod), algún ítem reservado puede quedarse parado en una tolva hasta que se libere sitio. No bloquea el resto de la torre. `/smarterhoppers diagnostico` indica dónde hay ítems sin destino.

## Compatibilidad
- **Lithium** (instalado):
  - Inyectar el código del mod en puntos que se sigan ejecutando con Lithium activo.
  - Despertar las tolvas "dormidas" cuando cambie una torre o se libere sitio.
  - Plan de emergencia: `mixin.block.hopper=false` en `lithium.properties`.
- **Easy Multi Item Sorter:** desinstalarlo. Los item frames con comparador se mantienen.
- **Fast Item Frames** (dependencia opcional, licencia MPL-2.0): convierte los item frames en **bloques** (`ItemFrameBlock` + `ItemFrameBlockEntity`). Siempre los nuevos y, si está activado `convertAllExistingItemFrames`, también los existentes. La detección del contenedor marcado debe aceptar las dos formas: la entidad vanilla y el bloque de Fast Item Frames.
  - Por verificar en la fase 0: si `ItemFrameBlockEntity` implementa `Container`, una tolva justo debajo podría succionar el comparador del item frame.
- **Revisados sin conflicto** (perfil Test, 50 mods): solo tocan pantallas, renderizado, generación o drops. malilib y Litematica solo intervienen al abrir un menú.
- **Para otros usuarios, por revisar:**
  - Riesgo alto: Carpet / Carpet Extra, mods que aceleran tolvas, mods de tuberías que usen la Transfer API, almacenamiento con terminal.
  - Riesgo medio: C2ME, Moonrise, ServerCore, VMP, Async.

## Librerías
- **Fabric API:** eventos, comandos, interacción con entidades y GameTest.
- **MixinExtras:** incluido en Fabric Loader.
- **Lithium:** dependencia solo de compilación, con mixins condicionales que solo se activan si está instalado.
- **Gson:** para la configuración; viene incluido en Minecraft.
- **spark:** solo para medir el rendimiento.

## Configuración (`config/smarter-hoppers.json`)
- `margen_cofre`
- `compat_lithium`: `auto` | `desactivar_optimizacion_tolvas`

## Fases
0. **Investigación:**
   - código de `HopperBlockEntity` e item frames en 26.2;
   - mixins de tolvas de Lithium y su posible API;
   - versiones de Fabric Loader, Fabric API y Loom;
   - lista de mods del usuario.
1. ✅ Proyecto base que carga en 26.2 con Lithium (build y GameTest en verde, 2026-11-25).
2. ✅ Detección de contenedores marcados (entidad vanilla y bloque de Fast Item Frames, cualquier contenedor, cofre doble por cualquiera de sus mitades) con caché invalidada por eventos y TTL de 100 ticks. 4 GameTests. La caché de la forma de las torres pasa a la fase 5, que es donde se usa.
3. ✅ R1, R2 y bloqueo de los item frames-bloque con comparador. Implementado sobre `canPlaceItem`/`canTakeItem` de los contenedores (barril, cofre, shulker, dispensador, dropper, tolva; el cofre doble cuenta sus dos mitades), así que funciona con y sin Lithium (13 GameTests en verde en ambos casos). Afecta también a droppers y transferencias de otros mods, no solo a tolvas. Pendiente para la fase 7: avisar a Lithium cuando un contenedor gana o pierde la marca, para que no deje tolvas dormidas.
4. ✅ R3: una tolva que no apunta hacia abajo y tiene un contenedor marcado justo debajo le mete primero lo que este acepte (R2); el resto sigue hacia delante. Inyección al inicio de `ejectItems`, antes que Lithium. 16 GameTests en verde con y sin Lithium (`-PnoLithium`).
5. ✅ Tolvas de dos salidas (apuntan de lado a cualquier contenedor, tolvas incluidas, y tienen otra tolva debajo): cada ítem va a la salida que ya lo contiene. Orden:
   1. contenedor de delante que no es tolva, si lo contiene y tiene sitio;
   2. tolva de abajo, si lo contiene y tiene sitio, o si un cofre más abajo de la columna lo contiene y tiene sitio (T3);
   3. tolva de delante, si lo contiene y tiene sitio. Si las dos salidas son tolvas y lo contienen, gana la de abajo (filtro de un sorter vanilla).
   - Si ninguna lo acepta pero un cofre de la columna lo contiene (su casa está llena) → espera en la tolva. Las tolvas llenas no hacen esperar (un filtro lleno no atasca la línea).
   - Ítem sin casa → comportamiento vanilla, para no romper torres de cofres vacíos.
   Sin caché de la forma de las torres: se medirá en la fase 9 antes de añadirla. 24 GameTests en verde con y sin Lithium, incluidos dos de sorter vanilla.
6. ✅ R4: un contenedor marcado con una tolva debajo solo acepta un ítem si la torre puede guardarlo, simulando lo que ya está en tránsito (contenedor marcado por encima de sus semillas + tolvas de la torre) con los huecos vacíos de cada cofre compartidos entre sus ítems. Los ítems sin casa en la torre cuentan contra los huecos vacíos de cualquier cofre. Resultado memorizado por tick. 23 GameTests en verde con y sin Lithium, incluido el caso del problema 2 (quedan 10 huecos, llegan 30 troncos: entran 10 y 20 van al overflow). El margen configurable queda para la fase 9.
7. ✅ Compatibilidad con Lithium: cuando cambia un contenedor de almacenaje (o su marca), se avisa a las tolvas que lo alimentan aguas arriba (las que le apuntan, la columna de encima, el contenedor marcado y la línea) con la API pública de Lithium (`changedInteractionConditions`), para que reintenten aunque estén dormidas. Todas las consultas a bloques vecinos usan `Loaded` y nunca cargan chunks (evita cuelgues al descargar chunks o cerrar el servidor). 26 GameTests en verde con y sin Lithium.
8. Comando `/smarterhoppers diagnostico`.
9. Configuración, casos límite y rendimiento.

## Pruebas (GameTest + manuales)
1. Filtro con madera, escaleras y vallas sobre una torre, con ítems mezclados por la línea.
2. Torre de madera llena, 40 stacks de madera y luego escaleras: las escaleras llegan a su cofre y la madera va al overflow.
3. Al cofre de madera le quedan 10 huecos y se mandan 64 maderas: entran exactamente 10 y 54 van al overflow.
4. Cofre mixto con 1 hueco libre y escaleras y vallas a la vez: solo entra lo que cabe.
5. R1: el filtro nunca baja de 2 ítems de cada tipo.
6. Inserción manual durante el tránsito: el resto de la torre sigue fluyendo.
7. Romper o cambiar el item frame: la marca se actualiza.
8. Todas las anteriores con Lithium activo.
9. Carga: 80 torres a pleno ritmo, medida con spark.
10. Salir y volver a entrar en el mundo con ítems en tránsito.
