package com.sangue.sangue

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal val VermelhoOriginal = Color(0xFFE21C2C)
internal val TextoOriginal = Color(0xFF1E293B)
internal val CinzaOriginal = Color(0xFF64748B)
internal val BordaOriginal = Color(0xFFE2E8F0)
internal val FundoOriginal = Color(0xFFF8FAFC)
internal val RosaOriginal = Color(0xFFFEF2F2)

@Composable
internal fun CabecalhoOriginal(
    titulo: String,
    onBack: () -> Unit,
    enabled: Boolean = true,
    acao: @Composable () -> Unit = {}
) {
    Box(Modifier.fillMaxWidth().background(Color.White).padding(top = 20.dp, bottom = 12.dp).heightIn(min = 48.dp)) {
        IconButton(onClick = onBack, enabled = enabled, modifier = Modifier.align(Alignment.CenterStart).padding(start = 8.dp).size(48.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", tint = TextoOriginal)
        }
        Text(titulo, modifier = Modifier.align(Alignment.Center).fillMaxWidth().padding(horizontal = 64.dp).semantics { heading() },
            color = TextoOriginal, fontSize = 18.sp, fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Box(Modifier.align(Alignment.CenterEnd).padding(end = 8.dp)) { acao() }
    }
}

@Composable
internal fun TelaOriginal(
    titulo: String,
    onBack: () -> Unit,
    enabled: Boolean = true,
    acao: @Composable () -> Unit = {},
    rodape: @Composable () -> Unit = {},
    conteudo: @Composable ColumnScope.() -> Unit
) {
    BackHandler { if (enabled) onBack() }
    Scaffold(
        modifier = Modifier.fillMaxSize().imePadding(), containerColor = Color.White,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { CabecalhoOriginal(titulo, onBack, enabled, acao) },
        bottomBar = rodape
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp), content = conteudo)
    }
}

@Composable
internal fun CartaoOriginal(
    modifier: Modifier = Modifier,
    destaque: Boolean = false,
    conteudo: @Composable ColumnScope.() -> Unit
) {
    Card(modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (destaque) Color(0xFFFECACA) else BordaOriginal),
        colors = CardDefaults.cardColors(containerColor = if (destaque) RosaOriginal else Color.White)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = conteudo)
    }
}

@Composable
internal fun BotaoOriginal(
    texto: String,
    enabled: Boolean = true,
    secundario: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val tamanho = modifier.fillMaxWidth().heightIn(min = 56.dp)
    if (secundario) OutlinedButton(onClick, modifier = tamanho, enabled = enabled, shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFFECACA)), colors = ButtonDefaults.outlinedButtonColors(contentColor = VermelhoOriginal)) {
        Text(texto, fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    } else Button(onClick, modifier = tamanho, enabled = enabled, shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = VermelhoOriginal, contentColor = Color.White)) {
        Text(texto, fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}

@Composable
internal fun RodapeOriginal(conteudo: @Composable ColumnScope.() -> Unit) {
    Surface(color = Color.White, shadowElevation = 3.dp) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp), content = conteudo)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AcessoOriginal(titulo: String, descricao: String, enabled: Boolean = true, onClick: () -> Unit) {
    Card(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, BordaOriginal), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(titulo, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextoOriginal)
                Text(descricao, fontSize = 12.sp, color = CinzaOriginal, lineHeight = 18.sp)
            }
            Spacer(Modifier.width(12.dp))
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = CinzaOriginal)
        }
    }
}

@Composable
internal fun SeloOriginal(texto: String, cor: Color = VermelhoOriginal) {
    Surface(color = cor.copy(alpha = 0.09f), shape = RoundedCornerShape(50)) {
        Text(texto, Modifier.padding(horizontal = 12.dp, vertical = 6.dp), color = cor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
internal fun PassosOriginais(etapa: Int, rotulos: List<String>) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        rotulos.forEachIndexed { i, rotulo ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                val completo = etapa > i + 1
                val atual = etapa == i + 1
                Surface(Modifier.size(36.dp), color = if (completo || atual) VermelhoOriginal else Color(0xFFF1F5F9), shape = CircleShape) {
                    Box(contentAlignment = Alignment.Center) {
                        if (completo) Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        else Text("${i + 1}", color = if (atual) Color.White else CinzaOriginal, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(rotulo, textAlign = TextAlign.Center, fontSize = 11.sp,
                    color = if (completo || atual) VermelhoOriginal else CinzaOriginal, fontWeight = if (atual) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
}

@Composable
internal fun AvatarOriginal(nome: String, modifier: Modifier = Modifier) {
    val iniciais = nome.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.take(2).joinToString("") { it.take(1).uppercase() }.ifBlank { "?" }
    Surface(modifier.size(52.dp), color = RosaOriginal, shape = CircleShape) {
        Box(contentAlignment = Alignment.Center) { Text(iniciais, color = VermelhoOriginal, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
    }
}