`risk-zones` se separa en dos subfeatures porque el backend expone dos casos de uso distintos.

`map/`
- consume `GET /api/mapa/zonas-riesgo/activas`
- es una vista para usuarios autenticados
- prioriza lectura, filtros y visualizacion geoespacial

`management/`
- consume `GET/POST/PUT/PATCH/DELETE /api/risk-zones`
- es una vista administrativa
- prioriza CRUD, validaciones y control de permisos

La separacion evita mezclar:
- permisos distintos
- modelos de UI distintos
- servicios con responsabilidades distintas
- rutas de usuario y rutas de administrador en el mismo modulo
