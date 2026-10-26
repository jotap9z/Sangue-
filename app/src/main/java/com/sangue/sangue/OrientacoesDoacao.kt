package com.sangue.sangue

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight

@Composable
fun OrientacoesDoacao() {
    val context = LocalContext.current
    Text("Orientações para a doação", fontWeight = FontWeight.Bold)
    Text("Apresente identificação oficial com foto. Informe seu histórico de saúde, procedimentos, medicamentos e doações feitas fora do aplicativo. O preparo inclui alimentação e descanso adequados; confirme as recomendações com a instituição.")
    Text("O questionário aponta situações que precisam de conversa com a equipe. Uma resposta sobre cirurgia, tatuagem ou piercing não define um prazo automático de impedimento. As regras dependem do procedimento e das condições em que foi realizado.")
    Text("A previsão de intervalo considera o histórico registrado. Ela não substitui avaliação clínica, exames, verificação dos critérios da faixa etária ou autorização de responsável quando aplicável.")
    Text("Referências oficiais consultadas em 25/10/2026. A Portaria GM/MS 11.685/2026 atualizou critérios, com vigência desde 30/09/2026.")
    listOf(
        "Ministério da Saúde — Doação de sangue" to "https://www.gov.br/saude/pt-br/composicao/saes/doacao-de-sangue",
        "Anvisa — Atualização dos procedimentos hemoterápicos" to "https://www.gov.br/anvisa/pt-br/assuntos/noticias-anvisa/2026/ministerio-da-saude-atualiza-procedimentos-hemoterapicos-e-anvisa-orienta-periodo-de-transicao"
    ).forEach { (titulo, url) -> TextButton(onClick = {
        try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
        catch (_: Exception) { Toast.makeText(context, "Não foi possível abrir o navegador.", Toast.LENGTH_SHORT).show() }
    }) { Text(titulo) } }
}