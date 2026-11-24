package pe.edu.upeu.megafarma.app.presentation.login

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import pe.edu.upeu.megafarma.app.presentation.tema.ColoresMarca
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import pe.edu.upeu.megafarma.app.presentation.componentes.Iconos

/*
 * Inicio de sesion. En pantallas anchas (web) se divide en un panel de marca
 * y el formulario; en el celular solo se muestra el formulario.
 */
@Composable
fun LoginScreen(viewModel: LoginViewModel = koinViewModel()) {
    val estado by viewModel.estado.collectAsState()

    // Surface fija el color del texto segun el tema (claro u oscuro)
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        if (maxWidth >= 900.dp) {
            Row(Modifier.fillMaxSize()) {
                PanelMarca(Modifier.weight(1f).fillMaxHeight())
                Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    Formulario(estado, viewModel, Modifier.widthIn(max = 420.dp).padding(32.dp))
                }
            }
        } else {
            Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Logo(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary)
                    Spacer(Modifier.height(12.dp))
                    Text("Megafarma POS", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(20.dp))
                    Surface(shape = MaterialTheme.shapes.large, tonalElevation = 1.dp, shadowElevation = 2.dp) {
                        Formulario(estado, viewModel, Modifier.widthIn(max = 420.dp).padding(24.dp))
                    }
                }
            }
        }
    }
    }
}

@Composable
private fun PanelMarca(modifier: Modifier) {
    Box(
        modifier.background(Brush.linearGradient(ColoresMarca.degradado)),
        contentAlignment = Alignment.Center
    ) {
        Column(Modifier.widthIn(max = 460.dp).padding(48.dp)) {
            Logo(Color.White.copy(alpha = 0.16f), Color.White)
            Spacer(Modifier.height(28.dp))
            Text("Megafarma POS", style = MaterialTheme.typography.displaySmall, color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Text(
                "Sistema de punto de venta e inventario para la botica Megafarma.",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.85f)
            )
            Spacer(Modifier.height(36.dp))
            Caracteristica(Iconos.Recibo, "Boleta y factura electrónica", "Serie, correlativo, IGV y XML UBL 2.1")
            Caracteristica(Iconos.Inventario, "Inventario por lotes", "Venta FEFO y alertas de vencimiento")
            Caracteristica(Iconos.Dashboard, "Dashboard en tiempo real", "Indicadores, reportes y comparativos")
            Caracteristica(Iconos.Escudo, "Acceso seguro por roles", "Cada trabajador ve solo lo que le corresponde")
        }
    }
}

@Composable
private fun Caracteristica(icono: ImageVector, titulo: String, detalle: String) {
    Row(Modifier.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) { Icon(icono, null, tint = Color.White, modifier = Modifier.size(20.dp)) }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(titulo, color = Color.White, fontWeight = FontWeight.SemiBold)
            Text(detalle, color = Color.White.copy(alpha = 0.75f), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun Logo(fondo: Color, color: Color) {
    Box(Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(fondo), contentAlignment = Alignment.Center) {
        Icon(Iconos.Farmacia, null, tint = color, modifier = Modifier.size(30.dp))
    }
}

@Composable
private fun Formulario(estado: LoginUiState, viewModel: LoginViewModel, modifier: Modifier) {
    var verClave by remember { mutableStateOf(false) }
    var verAyuda by remember { mutableStateOf(false) }
    var toques by remember { mutableStateOf(0) }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            "Iniciar sesión",
            Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                toques++
                if (toques >= 5) {
                    toques = 0
                    viewModel.alternarServidor()
                }
            },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text("Ingrese con su usuario asignado", color = MaterialTheme.colorScheme.onSurfaceVariant)

        OutlinedTextField(
            value = estado.usuario,
            onValueChange = viewModel::cambiarUsuario,
            label = { Text("Usuario") },
            leadingIcon = { Icon(Iconos.Persona, null, Modifier.size(20.dp)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = estado.clave,
            onValueChange = viewModel::cambiarClave,
            label = { Text("Contraseña") },
            leadingIcon = { Icon(Iconos.Candado, null, Modifier.size(20.dp)) },
            trailingIcon = {
                IconButton(onClick = { verClave = !verClave }) {
                    Icon(if (verClave) Iconos.Ocultar else Iconos.Ver, if (verClave) "Ocultar" else "Mostrar", Modifier.size(20.dp))
                }
            },
            singleLine = true,
            visualTransformation = if (verClave) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { viewModel.ingresar() }),
            modifier = Modifier.fillMaxWidth()
        )

        if (estado.error != null) {
            Surface(color = MaterialTheme.colorScheme.errorContainer, shape = MaterialTheme.shapes.small) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Iconos.Advertencia, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(estado.error!!, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        Button(
            onClick = viewModel::ingresar,
            enabled = !estado.cargando,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            if (estado.cargando) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
            else Text("Ingresar", fontWeight = FontWeight.SemiBold)
        }

        TextButton(onClick = { verAyuda = !verAyuda }, contentPadding = PaddingValues(0.dp)) {
            Text("¿Olvidó su contraseña?", style = MaterialTheme.typography.bodyMedium)
        }
        if (verAyuda) {
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.small) {
                Text(
                    "Pida al administrador que restablezca su contraseña desde Usuarios. Le dará una contraseña " +
                        "temporal y el sistema le pedirá crear una nueva al ingresar.",
                    Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        // Direccion del backend: solo para pruebas en emulador o celular (se abre tocando 5 veces el titulo).
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
    }
}
