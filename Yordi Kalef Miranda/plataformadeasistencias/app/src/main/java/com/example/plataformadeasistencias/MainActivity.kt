package com.example.plataformadeasistencias

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

private const val BASE_URL = "http://192.168.3.8:8080/asistencias_api/"

data class UsuarioDTO(
    val id: Int? = null,
    val usuario: String,
    val contrasena: String? = null,
    val nombre_completo: String,
    val correo: String? = null
)

data class RespSimple(val success: Boolean, val id: Int? = null, val usuario: Map<String, Any>? = null, val msg: String? = null)
data class UsuariosResp(val success: Boolean, val usuarios: List<Map<String, Any>>? = null)

interface ApiService {
    @POST("login.php")
    suspend fun login(@Body body: Map<String, String>): RespSimple

    @GET("obtener_usuarios.php")
    suspend fun obtenerUsuarios(): UsuariosResp

    @POST("agregar_usuario.php")
    suspend fun agregarUsuario(@Body body: Map<String, String>): RespSimple

    @PUT("actualizar_usuario.php")
    suspend fun actualizarUsuario(@Body body: Map<String, String>): RespSimple

    @HTTP(method = "DELETE", path = "eliminar_usuario.php", hasBody = true)
    suspend fun eliminarUsuario(@Body body: Map<String, String>): RespSimple
}

class MainActivity : ComponentActivity() {
    private val api: ApiService by lazy {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ContenidoApp(api)
                }
            }
        }
    }
}

@Composable
fun ContenidoApp(api: ApiService) {
    var pantalla by remember { mutableStateOf("login") } // login, lista
    var usuarioActual by remember { mutableStateOf<Map<String, Any>?>(null) }

    when (pantalla) {
        "login" -> PantallaLogin(api, onIngreso = { u ->
            usuarioActual = u
            pantalla = "lista"
        })
        "lista" -> PantallaUsuarios(api, onCerrar = {
            usuarioActual = null
            pantalla = "login"
        })
    }
}

@Composable
fun PantallaLogin(api: ApiService, onIngreso: (Map<String, Any>) -> Unit) {
    val ctx = LocalContext.current
    var usuario by remember { mutableStateOf("") }
    var contrasena by remember { mutableStateOf("") }
    var mostrarContrasena by remember { mutableStateOf(false) }
    var cargando by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.Center) {
        Text("Plataforma de Asistencias", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(bottom = 16.dp))
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(value = usuario, onValueChange = { usuario = it }, label = { Text("Usuario") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = contrasena,
                    onValueChange = { contrasena = it },
                    label = { Text("Contraseña") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (mostrarContrasena) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { mostrarContrasena = !mostrarContrasena }) {
                            Icon(if (mostrarContrasena) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null)
                        }
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = {
                    if (usuario.isBlank() || contrasena.isBlank()) {
                        Toast.makeText(ctx, "Completa las credenciales", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    cargando = true
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val resp = api.login(mapOf("usuario" to usuario, "contrasena" to contrasena))
                            CoroutineScope(Dispatchers.Main).launch {
                                cargando = false
                                if (resp.success && resp.usuario != null) {
                                    onIngreso(resp.usuario)
                                } else {
                                    Toast.makeText(ctx, resp.msg ?: "Usuario o contraseña incorrectos", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } catch (e: Exception) {
                            CoroutineScope(Dispatchers.Main).launch {
                                cargando = false
                                Toast.makeText(ctx, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (cargando) "Ingresando..." else "Ingresar")
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Botón de acceso sin login
                TextButton(onClick = {
                    // Usuario demo temporal
                    onIngreso(mapOf("id" to 0, "usuario" to "demo", "nombre_completo" to "Acceso Demo", "correo" to "demo@correo.com"))
                }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text("Acceder sin login")
                }
            }
        }
    }
}

@Composable
fun PantallaUsuarios(api: ApiService, onCerrar: () -> Unit) {
    val ctx = LocalContext.current
    var usuarios by remember { mutableStateOf(listOf<Map<String, Any>>()) }
    var cargando by remember { mutableStateOf(false) }
    var mostrarAgregar by remember { mutableStateOf(false) }
    var usuarioEditar by remember { mutableStateOf<Map<String, Any>?>(null) }

    fun cargarUsuarios() {
        cargando = true
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val resp = api.obtenerUsuarios()
                CoroutineScope(Dispatchers.Main).launch {
                    cargando = false
                    usuarios = resp.usuarios ?: emptyList()
                }
            } catch (e: Exception) {
                CoroutineScope(Dispatchers.Main).launch {
                    cargando = false
                    Toast.makeText(ctx, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    LaunchedEffect(Unit) { cargarUsuarios() }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Usuarios", style = MaterialTheme.typography.headlineSmall)
            Row {
                Button(onClick = { mostrarAgregar = true }) { Text("Agregar") }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { onCerrar() }) { Text("Cerrar sesión") }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (cargando) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Column
        }

        if (mostrarAgregar) {
            FormUsuario(onCancelar = { mostrarAgregar = false }, onGuardado = {
                cargarUsuarios()
                mostrarAgregar = false
            }, api = api)
        }

        usuarioEditar?.let { u ->
            FormUsuario(editar = u, onCancelar = { usuarioEditar = null }, onGuardado = {
                cargarUsuarios()
                usuarioEditar = null
            }, api = api)
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(usuarios) { usuario ->
                Card(modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp), shape = RoundedCornerShape(8.dp)) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text((usuario["nombre_completo"] as? String) ?: "Sin nombre", style = MaterialTheme.typography.titleMedium)
                            Text((usuario["usuario"] as? String) ?: "", style = MaterialTheme.typography.bodySmall)
                            Text((usuario["correo"] as? String) ?: "", style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = { usuarioEditar = usuario }) { Icon(Icons.Default.Edit, contentDescription = "Editar") }
                        IconButton(onClick = {
                            CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    val resp = api.eliminarUsuario(mapOf("id" to (usuario["id"].toString())))
                                    CoroutineScope(Dispatchers.Main).launch {
                                        if (resp.success) {
                                            Toast.makeText(ctx, "Usuario eliminado", Toast.LENGTH_SHORT).show()
                                            cargarUsuarios()
                                        } else {
                                            Toast.makeText(ctx, "Error: ${resp.msg}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                } catch (e: Exception) {
                                    CoroutineScope(Dispatchers.Main).launch {
                                        Toast.makeText(ctx, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }) { Icon(Icons.Default.Delete, contentDescription = "Eliminar") }
                    }
                }
            }
        }
    }
}

@Composable
fun FormUsuario(editar: Map<String, Any>? = null, onCancelar: () -> Unit, onGuardado: () -> Unit, api: ApiService) {
    val ctx = LocalContext.current
    var usuario by remember { mutableStateOf(editar?.get("usuario")?.toString() ?: "") }
    var nombre by remember { mutableStateOf(editar?.get("nombre_completo")?.toString() ?: "") }
    var correo by remember { mutableStateOf(editar?.get("correo")?.toString() ?: "") }
    var contrasena by remember { mutableStateOf("") }
    var mostrarContrasena by remember { mutableStateOf(false) }
    var guardando by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(if (editar == null) "Agregar usuario" else "Editar usuario", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(value = nombre, onValueChange = { nombre = it }, label = { Text("Nombre completo") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = usuario, onValueChange = { usuario = it }, label = { Text("Usuario") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = correo, onValueChange = { correo = it }, label = { Text("Correo") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = contrasena,
                onValueChange = { contrasena = it },
                label = { Text(if (editar == null) "Contraseña" else "Nueva contraseña (opcional)") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (mostrarContrasena) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { mostrarContrasena = !mostrarContrasena }) {
                        Icon(if (mostrarContrasena) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null)
                    }
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onCancelar) { Text("Cancelar") }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = {
                    if (nombre.isBlank() || usuario.isBlank() || (editar == null && contrasena.isBlank())) {
                        Toast.makeText(ctx, "Rellena los campos obligatorios", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    guardando = true
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            if (editar == null) {
                                val resp = api.agregarUsuario(mapOf(
                                    "usuario" to usuario,
                                    "contrasena" to contrasena,
                                    "nombre_completo" to nombre,
                                    "correo" to (correo ?: "")
                                ))
                                CoroutineScope(Dispatchers.Main).launch {
                                    guardando = false
                                    if (resp.success) {
                                        Toast.makeText(ctx, "Usuario creado", Toast.LENGTH_SHORT).show()
                                        onGuardado()
                                    } else {
                                        Toast.makeText(ctx, "Error: ${resp.msg}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else {
                                val id = editar["id"].toString()
                                val body = mutableMapOf("id" to id, "nombre_completo" to nombre, "correo" to (correo ?: ""), "usuario" to usuario)
                                if (contrasena.isNotBlank()) body["contrasena"] = contrasena
                                val resp = api.actualizarUsuario(body)
                                CoroutineScope(Dispatchers.Main).launch {
                                    guardando = false
                                    if (resp.success) {
                                        Toast.makeText(ctx, "Usuario actualizado", Toast.LENGTH_SHORT).show()
                                        onGuardado()
                                    } else {
                                        Toast.makeText(ctx, "Error: ${resp.msg}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            CoroutineScope(Dispatchers.Main).launch {
                                guardando = false
                                Toast.makeText(ctx, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }) {
                    Text(if (guardando) "Guardando..." else "Guardar")
                }
            }
        }
    }
}
