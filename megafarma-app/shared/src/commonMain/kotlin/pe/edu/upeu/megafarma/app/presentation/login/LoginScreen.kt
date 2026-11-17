package pe.edu.upeu.megafarma.app.presentation.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LoginScreen(viewModel: LoginViewModel = koinViewModel()) {
    val estado by viewModel.estado.collectAsState()

    Box(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState()).padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(Modifier.widthIn(max = 420.dp).fillMaxWidth()) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Megafarma",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text("Ventas e inventario de la botica", color = MaterialTheme.colorScheme.onSurfaceVariant)

                OutlinedTextField(
                    value = estado.usuario,
                    onValueChange = viewModel::cambiarUsuario,
                    label = { Text("Usuario") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = estado.clave,
                    onValueChange = viewModel::cambiarClave,
                    label = { Text("Contraseña") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { viewModel.ingresar() }),
                    modifier = Modifier.fillMaxWidth()
                )

                TextButton(onClick = viewModel::alternarServidor, contentPadding = PaddingValues(0.dp)) {
                    Text("Servidor: ${estado.servidor}", style = MaterialTheme.typography.bodySmall)
                }
                if (estado.verServidor) {
                    OutlinedTextField(
                        value = estado.servidor,
                        onValueChange = viewModel::cambiarServidor,
                        label = { Text("Dirección del backend") },
                        supportingText = { Text("Emulador: http://10.0.2.2:8080 · Celular: http://IP-de-la-laptop:8080") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (estado.error != null) {
                    Text(estado.error!!, color = MaterialTheme.colorScheme.error)
                }

                Button(
                    onClick = viewModel::ingresar,
                    enabled = !estado.cargando,
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    if (estado.cargando) {
                        CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Ingresar")
                    }
                }

                Text(
                    "Usuarios de prueba: admin / Admin123 · vendedor / Venta123 · almacen / Almacen123",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
