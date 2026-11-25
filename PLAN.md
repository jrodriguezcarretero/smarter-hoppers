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
- **Contenedor marcado:** contenedor con un item frame (normal o luminoso; como entidad vanilla o como bloque de Fast Item Frames) en cualquier cara con un comparador dentro. En un cofre doble basta con que una mitad esté marcada.
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
2. Detección de contenedores marcados y caché de la forma de las torres.
3. R1 y R2.
4. R3: prioridad de la tolva de la línea sobre el contenedor marcado.
5. Torre: tolvas de una o dos salidas, T1 y T2.
6. R4 con reservas, fondo común y margen, más la anticipación T3.
7. Compatibilidad con Lithium.
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
