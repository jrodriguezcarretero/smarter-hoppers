# Fase 0: investigación

Fecha: 2026-11-25. Fuentes:
- `HopperBlockEntity`, `ItemFrame`, `Container`, `ListBackedContainer` y `ChestBlock`, descompilados con Vineflower 1.11.2 desde el jar de 26.2 (perfil Test de Modrinth);
- el código de Fast Item Frames (rama `26.2.x`) y de Lithium en GitHub;
- los metadatos de fabricmc.net.

## 1. Versiones del entorno de desarrollo
| Pieza | Versión | Nota |
|---|---|---|
| Minecraft | 26.2 | Sin ofuscar: se programa con los nombres de Mojang |
| Fabric Loader | 0.20.1 (última estable) | El perfil Test usa **0.19.5** → el mod declarará `fabricloader >= 0.19.5` |
| Fabric API | 0.161.0+26.2 | Es la que tiene instalada el perfil Test |
| Loom | plugin `net.fabricmc.fabric-loom`, `1.19.4` (fijada; el ejemplo usa `1.19-SNAPSHOT`) | Sin `mappings` ni `modImplementation` |
| Gradle | 9.8.2 (wrapper) | |
| Java | 25 | **Solo tienes el runtime (JRE) de Java 25. Para compilar hace falta un JDK 25** |

La base es la plantilla `fabric-example-mod`, rama `26.2`.

## 2. Tolvas en vanilla 26.2 (`HopperBlockEntity`)
Flujo de cada tick (`pushItemsTick` → `tryMoveItems`):
1. Si el cooldown ha terminado y la tolva está habilitada (`ENABLED`), primero **empuja** (`ejectItems`) y luego **succiona** (`suckInItems`).
2. Si algo se ha movido, aplica un cooldown de 8 ticks.

| Método | Qué hace | Uso en el mod |
|---|---|---|
| `ejectItems(level, pos, self)` | Busca el destino con `getAttachedContainer`; si `isFullContainer` es cierto, aborta. Si no, **prueba todos sus huecos** hasta que uno entra | **R3** (prioridad hacia el contenedor marcado de debajo), **T1** (empujar al cofre solo si ya contiene el ítem), **T3** |
| `suckInItems(level, hopper)` | Busca un contenedor encima (`getSourceContainer`) y prueba hueco a hueco con `tryTakeInItemFromSlot`; si no hay contenedor, recoge entidades de ítem | Punto de paso de R1 y T2 |
| `canTakeItemFromContainer(into, from, stack, slot, dir)` | Recibe la tolva destino y el contenedor origen | **R1, T2** y la protección del item frame |
| `tryMoveInItem(from, container, stack, slot, dir)` → `canPlaceItemInContainer(...)` | Comprueba cada hueco al insertar; tiene acceso a origen y destino | **R2** y la protección del item frame |
| `Container.canTakeItem(into, slot, stack)` / `canPlaceItem(slot, stack)` | Métodos por defecto de la interfaz | Alternativa a los anteriores |

Conclusiones:
- Todas las reglas tienen un punto de enganche limpio en vanilla: **sin `@Overwrite`**, solo inyecciones de MixinExtras (`@WrapMethod` / `@ModifyReturnValue` / `@WrapOperation`).
- Que la tolva "prueba todos sus huecos al empujar" queda **confirmado** (bucle en `ejectItems`). El plan se apoya en ello.
- Los cofres dobles llegan como `CompoundContainer` a través de `ChestBlock.getContainer(...)`: el mod los trata como un único inventario.
- `ItemFrame` (entidad): el ítem se obtiene con `getItem()`; la pared en la que está colgado es `pos.relative(getDirection().getOpposite())`. Al cambiar el ítem, `setItem` llama a `updateNeighbourForOutputSignal`, que sirve para invalidar la caché.

## 3. Fast Item Frames 26.2.x
- **Bloques:** `fastitemframes:item_frame` y `fastitemframes:glow_item_frame`. Comparten la block entity `fastitemframes:item_frame`.
- `FACING` indica hacia dónde mira el item frame; el bloque en el que está colgado es `pos.relative(FACING.getOpposite())`. El ítem está en el hueco 0 (`getItem()`).
- **Conversión de entidad a bloque:** solo ocurre con los item frames recién colocados, o con todos al cargar chunks si `convertAllExistingItemFrames=true` (por defecto está desactivado). **No hay conversión inversa.** Por tanto, en un mundo conviven las dos formas.
- ⚠️ **Fallo real:** el bloque es un `Container` (`ListBackedContainer`) sin restricciones. Una tolva **debajo** del item frame-bloque le succiona el comparador, y una tolva que apunte a él puede meterle ítems.
  - En el montaje del usuario, T1 está justo debajo del item frame. Le afecta si el item frame es de tipo bloque.
  - **Solución en el mod:** un item frame con comparador (en cualquiera de las dos formas) no deja sacar ni meter nada.
- **Detección sin dependencia de compilación:** comprobar el id del bloque o el tag `fastitemframes:item_frames` y leer el hueco 0 como `Container`. Los cambios se detectan con `setChanged` o con las actualizaciones de bloque y de comparador.

## 4. Lithium 0.25.3 (rama `26.2`)
Mixin en `HopperBlockEntity` con prioridad 950, sin `@Overwrite`:
- **Empujar (`ejectItems`):** inyecta en la llamada a `isFullContainer` y, en la práctica, **reemplaza el resto del método**. No se ejecutan `addItem`, `tryMoveInItem` ni `canPlaceItemInContainer`. Solo llama a `to.canPlaceItem(...)` y a `canPlaceItemThroughFace`.
- **Succionar (`suckInItems`):** si el origen es un inventario optimizado por Lithium, sustituye el bucle vanilla, **pero sigue llamando a `canTakeItemFromContainer`**.
- **Sustituye por cachés:** `getAttachedContainer`, `getSourceContainer`, `getItemsAtAndAbove`, `inventoryFull` e `isEmpty`.
- **Se salta intentos:** si ni la tolva ni su destino han cambiado (contador de modificaciones), no vuelve a intentarlo.
- **Duerme tolvas:** las tolvas inactivas dejan de procesarse por completo. Se despiertan cuando cambia el contenido de algo que vigilan, por una actualización de bloque vecino o cuando aparece un comparador.
- **API pública:** `LithiumDefaultedList.changedInteractionConditions()`. Se llama cuando cambia la regla de aceptación de un inventario sin que cambie su contenido; con eso se invalida la caché y se despiertan las tolvas.
- Configuración: `mixin.block.hopper` y `mixin.world.block_entity_ticking.sleeping.hopper`.

**Qué implica:**
- No se puede depender de `tryMoveInItem` ni de `canPlaceItemInContainer`: con Lithium no se ejecutan.
- `canTakeItemFromContainer` sí es fiable en los dos casos.
- No hay que usar `@Redirect` en los mismos puntos que Lithium; donde haga falta, `@WrapOperation`.

## 5. Decisiones para la fase 1
1. **Las tolvas gestionadas se toman al principio del método.** Inyección al inicio de `ejectItems` y `suckInItems`, que se ejecuta antes que la de Lithium.
   - Para las tolvas de dos salidas (L y T1–T3), el mod decide y mueve el ítem él mismo, y cancela lo demás.
   - El resto de tolvas ni se tocan: siguen funcionando con vanilla o con Lithium. Así se cumple la regla de "una salida = sin cálculos" y el mod no interfiere con nada más.
2. **R1 y la protección del item frame** se aplican sobre `canTakeItemFromContainer`, que llaman tanto vanilla como Lithium.
3. **R2 y la protección contra meter ítems en el item frame:** como Lithium no ejecuta `canPlaceItemInContainer`, se resuelven en el mismo enganche al inicio de `ejectItems`. Si el destino es un contenedor marcado o un item frame con comparador, lo gestiona el mod.
4. **Las tolvas gestionadas no duermen.** Su decisión depende de cofres a los que Lithium no está atento. Un mixin de compatibilidad, que solo se activa si Lithium está instalado, evita que entren en reposo.
   - Coste: unas 400 tolvas más procesándose cada tick. Con la caché de la forma de cada torre es despreciable.
   - Además, cuando cambian las condiciones (marca de un contenedor o cambio en una torre) se llama a `changedInteractionConditions()`.
5. **Fast Item Frames sin dependencia de compilación:** se detecta por el id o el tag del bloque y se lee como un `Container`.
6. **Caché de la forma de cada torre:** se invalida con las actualizaciones de bloques vecinos y de comparador. Esto cubre los cambios en item frames vanilla (`updateNeighbourForOutputSignal`) y los de Fast Item Frames (`setBlock` / `setChanged`).
7. **Base del proyecto:** plantilla `fabric-example-mod` (rama 26.2), Loom 1.19.4 y Gradle 9.8.2. Mod de servidor con `environment: "*"`, que no exige instalarlo en el cliente. Requiere `fabricloader >= 0.19.5`.

## 6. Requisitos antes de la fase 1
- **JDK 25**: ✅ ya está instalado en `D:\Java\jdk-25.0.2` (Oracle, incluye `javac`). No está en el PATH. Gradle lo usará a través de `org.gradle.java.home` (en `gradle.properties` del usuario) o de `JAVA_HOME` al ejecutar.
