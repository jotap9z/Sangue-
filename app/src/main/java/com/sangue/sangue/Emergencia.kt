package com.sangue.sangue

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergenciaScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToAgendar: () -> Unit = {},
) {
    val sangueRed = Color(0xFFE21C2C)
    val textDark = Color(0xFF1E293B)
    val textGray = Color(0xFF64748B)
    val borderGray = Color(0xFFF1F5F9)
    val backgroundGray = Color(0xFFF8FAFC)
    val alertBg = Color(0xFFFEF2F2)

    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    var showShareSheet by remember { mutableStateOf(value = false) }

    val mensagemCompartilhamento = "Doe sangue e ajude a salvar vidas com o Sangue+. Precisamos de doadores O- com urgência!"

    Scaffold(
        containerColor = backgroundGray,
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 16.dp, start = 16.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", tint = textDark) }
                Text("Emergência", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textDark, modifier = Modifier.weight(1f).offset(x = (-24).dp), textAlign = TextAlign.Center)
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
            Spacer(modifier = Modifier.height(8.dp))

            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = alertBg), shape = RoundedCornerShape(20.dp)) {
                Row(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Precisamos de doadores\ncom urgência!", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = sangueRed, lineHeight = 24.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(buildAnnotatedString { append("Tipo sanguíneo: "); withStyle(SpanStyle(color = sangueRed, fontWeight = FontWeight.Bold)) { append("O-") } }, fontSize = 14.sp, color = textDark)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Região: São Paulo - SP", fontSize = 14.sp, color = textDark, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Precisamos de você!", fontSize = 14.sp, color = textDark, fontWeight = FontWeight.SemiBold)
                    }
                    Box(modifier = Modifier.size(70.dp), contentAlignment = Alignment.Center) {
                        Image(painter = painterResource(id = R.drawable.sirene), contentDescription = "Alerta", modifier = Modifier.size(60.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp), elevation = CardDefaults.cardElevation(4.dp)) {
                Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                    Text("Você está elegível para doar?", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textDark)
                    Spacer(modifier = Modifier.height(20.dp))
                    ItemElegibilidade(Icons.Filled.CheckCircle, Color(0xFF22C55E), "Você pode doar", "")
                    Spacer(modifier = Modifier.height(16.dp))
                    ItemElegibilidade(Icons.Outlined.LocationOn, textGray, "A 3,2 km de distância", "")
                    Spacer(modifier = Modifier.height(16.dp))
                    ItemElegibilidade(Icons.Outlined.WaterDrop, sangueRed, "Compatível: ", "O-")

                    Spacer(modifier = Modifier.height(32.dp))
                    Button(onClick = onNavigateToAgendar, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = sangueRed), shape = RoundedCornerShape(16.dp)) { Text("Quero ajudar agora", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(onClick = { showShareSheet = true }, modifier = Modifier.fillMaxWidth().height(56.dp), border = BorderStroke(1.dp, borderGray), shape = RoundedCornerShape(16.dp)) { Text("Compartilhar", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textDark) }
                }
            }
            Spacer(modifier = Modifier.height(40.dp))
        }

        if (showShareSheet) {
            ModalBottomSheet(onDismissRequest = { showShareSheet = false }, sheetState = sheetState, containerColor = Color.White) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Compartilhar chamado", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = textDark, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
                    Spacer(modifier = Modifier.height(24.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        ShareAppButton(
                            name = "WhatsApp",
                            iconRes = R.drawable.whatsappicon,
                            outerColor = Color(0xFFDCFCE7),
                        ) {
                            shareToApp(context, "com.whatsapp", mensagemCompartilhamento)
                        }
                        ShareAppButton(
                            name = "Instagram",
                            iconRes = R.drawable.instaicon,
                            outerColor = Color(0xFFFCE7F3),
                        ) {
                            shareToApp(context, "com.instagram.android", mensagemCompartilhamento)
                        }
                        ShareAppButton(
                            name = "Mensagens",
                            imageVector = Icons.AutoMirrored.Outlined.Chat,
                            iconTint = Color(0xFF3B82F6),
                            outerColor = Color(0xFFDBEAFE),
                        ) {
                            sendSms(context, mensagemCompartilhamento)
                        }
                        ShareAppButton(
                            name = "Copiar link",
                            imageVector = Icons.Outlined.ContentCopy,
                            iconTint = Color(0xFF64748B),
                            outerColor = Color(0xFFF1F5F9),
                        ) {
                            copyToClipboard(context, mensagemCompartilhamento)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = alertBg), border = BorderStroke(1.dp, Color(0xFFFECACA)), shape = RoundedCornerShape(16.dp)) {
                        Text("Doe sangue e ajude a salvar vidas\ncom o Sangue+.", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textDark, modifier = Modifier.padding(20.dp), lineHeight = 20.sp)
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    TextButton(onClick = { coroutineScope.launch { sheetState.hide() }.invokeOnCompletion { showShareSheet = false } }, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                        Text("Cancelar", color = textGray, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ItemElegibilidade(icone: ImageVector, corIcone: Color, texto: String, destaque: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icone, contentDescription = null, tint = corIcone, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            buildAnnotatedString {
                append(texto)
                if (destaque.isNotEmpty()) {
                    withStyle(SpanStyle(color = Color(0xFFE21C2C), fontWeight = FontWeight.Bold)) {
                        append(destaque)
                    }
                }
            },
            fontSize = 14.sp,
            color = Color(0xFF1E293B),
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun ShareAppButton(
    name: String,
    iconRes: Int? = null,
    imageVector: ImageVector? = null,
    outerColor: Color,
    iconTint: Color = Color.Unspecified,
    onClick: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }) {
        Box(modifier = Modifier.size(60.dp).background(outerColor, CircleShape), contentAlignment = Alignment.Center) {
            when {
                iconRes != null -> {
                    Image(painter = painterResource(id = iconRes), contentDescription = name, modifier = Modifier.size(32.dp))
                }
                imageVector != null -> {
                    Icon(imageVector = imageVector, contentDescription = name, tint = iconTint, modifier = Modifier.size(28.dp))
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
    }
}

private fun shareToApp(context: Context, packageName: String, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
        setPackage(packageName)
    }
    try {
        context.startActivity(intent)
    } catch (_: Exception) {
        context.startActivity(Intent.createChooser(intent, "Compartilhar via"))
    }
}

private fun sendSms(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        data = "sms:".toUri()
        putExtra("sms_body", text)
    }
    try {
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(context, "App não encontrado", Toast.LENGTH_SHORT).show()
    }
}
private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Link", text))
    Toast.makeText(context, "Copiado!", Toast.LENGTH_SHORT).show()
}
