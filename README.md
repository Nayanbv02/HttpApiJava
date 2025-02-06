# SimpleHttpServer

Este es un servidor HTTP básico en Java que gestiona un endpoint `/books` con operaciones CRUD usando MySQL y sockets.

## Configuración

1. Instalar MySQL y ejecutar el script `schema.sql`.
2. Configurar credenciales en `DatabaseManager.java`.
3. Compilar y ejecutar el servidor.

```sh
javac -cp .:json.jar SimpleHttpServer.java
java SimpleHttpServer
```

## Endpoints
- `GET /books`: Devuelve todos los libros en JSON.
- `GET /books/ID`: Devuelve el libro que tenga el ID introducido en JSON.
- `DELETE /books/ID`: Elimina el libro que tenga el ID introducido.
- `POST /books`: Insercion de libros en formato JSON.
- `PUT /books/ID`: Sobreescribe un libro por ID con la informacion en formato JSON que se la pasa.
