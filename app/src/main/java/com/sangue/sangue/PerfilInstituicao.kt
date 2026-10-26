package com.sangue.sangue

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
internal fun PerfilInstituicaoScreen(onBack: () -> Unit) {
    val acesso = LocalAcessoHemocentro.current
    val scope = rememberCoroutineScope()
    val id = SangueApi.usuario?.optLong("hemocentroId")
    var dados by remember(id) { mutableStateOf<JSONObject?>(null) }
    var nome by remember(id) { mutableStateOf("") }
    var endereco by remember(id) { mutableStateOf("") }
    var cidade by remember(id) { mutableStateOf("") }
    var estado by remember(id) { mutableStateOf("") }
    var telefone by remember(id) { mutableStateOf("") }
    var cnpj by remember(id) { mutableStateOf("") }
    var cnes by remember(id) { mutableStateOf("") }
    var email by remember(id) { mutableStateOf("") }
    var carregando by remember(id) { mutableStateOf(true) }
    var salvando by remember(id) { mutableStateOf(false) }
    var conferir by remember(id) { mutableStateOf(false) }
    var editar by remember(id) { mutableStateOf(false) }
    var erro by remember(id) { mutableStateOf<String?>(null) }
    var mensagem by remember(id) { mutableStateOf<String?>(null) }
    var atualizar by remember(id) { mutableStateOf(0) }
    fun preencher(r: JSONObject) {
        dados = r
        fun campo(chave: String) = if (r.isNull(chave)) "" else r.optString(chave)
        nome = campo("nome"); endereco = campo("endereco"); cidade = campo("cidade"); estado = campo("estado")
        telefone = campo("telefone").filter { it.isDigit() }; cnpj = campo("cnpj").filter { it.isLetterOrDigit() }
        cnes = campo("cnes"); email = campo("emailContato")
    }
    LaunchedEffect(id, atualizar) {
        carregando = true
        try {
            preencher(SangueApi.objeto("GET", "/api/hemocentros/me"))
            conferir = false; editar = false; erro = null
        } catch (e: CancellationException) { throw e } catch (e: Exception) { erro = e.message ?: "Não foi possível consultar a instituição." } finally { carregando = false }
    }
    val ocupado = carregando || salvando
    val habilitado = editar && acesso.administrador && !ocupado && !conferir
    TelaOriginal("Perfil da instituição", onBack, enabled = !salvando, rodape = {
        if (dados != null && acesso.administrador) RodapeOriginal {
            if (!editar) BotaoOriginal("Editar dados", enabled = !ocupado && !conferir) { editar = true; mensagem = null }
            else BotaoOriginal(if (salvando) "Salvando…" else "Salvar alterações", enabled = habilitado) {
                if (nome.isBlank() || endereco.isBlank() || cidade.isBlank() || estado.length != 2 || telefone.length !in 10..11 || cnes.isBlank()) {
                    erro = "Preencha os campos obrigatórios e informe um telefone com DDD."
                } else {
                    salvando = true; erro = null
                    scope.launch {
                        try {
                            val corpo = JSONObject().put("nome", nome.trim()).put("endereco", endereco.trim())
                                .put("cidade", cidade.trim()).put("estado", estado.uppercase())
                                .put("telefone", telefone).put("cnpj", cnpj.ifBlank { null } ?: JSONObject.NULL)
                                .put("cnes", cnes.trim()).put("emailContato", email.trim().ifBlank { null } ?: JSONObject.NULL)
                            preencher(SangueApi.objeto("PUT", "/api/hemocentros/me", corpo))
                            editar = false; mensagem = "Dados atualizados."
                        } catch (e: CancellationException) { throw e } catch (e: Exception) {
                            erro = e.message ?: "Não foi possível salvar."
                            conferir = e !is ApiException || e.status == 0 || e.status >= 500
                        } finally { salvando = false }
                    }
                }
            }
        }
    }) {
        if (ocupado) LinearProgressIndicator(Modifier.fillMaxWidth(), color = VermelhoOriginal)
        ErroRecurso(erro)
        mensagem?.let { SeloOriginal(it) }
        if (dados != null) {
            CartaoOriginal(destaque = true) {
                AvatarOriginal(dados!!.optString("nome"))
                Text(dados!!.optString("nome"), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextoOriginal)
                Text("Informações exibidas aos doadores", color = CinzaOriginal)
            }
            if (!acesso.administrador) Text("Seu acesso permite consultar os dados da instituição.", color = CinzaOriginal)
            FormTextField("Nome da instituição", "Nome", value = nome, enabled = habilitado, isRequired = true) { nome = it.take(150) }
            FormTextField("CNPJ", "CNPJ cadastrado", value = cnpj, enabled = false, mascara = "##.###.###/####-##", aceitarLetrasNaMascara = true)
            FormTextField("CNES", "Cadastro nacional", value = cnes, enabled = habilitado, isRequired = true, keyboardType = KeyboardType.Number) { cnes = it.filter(Char::isDigit).take(7) }
            FormTextField("Endereço", "Endereço completo", value = endereco, enabled = habilitado, isRequired = true) { endereco = it.take(255) }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FormTextField("Cidade", "Cidade", modifier = Modifier.weight(2f), value = cidade, enabled = habilitado, isRequired = true) { cidade = it.take(100) }
                FormTextField("Estado", "UF", modifier = Modifier.weight(1f), value = estado, enabled = habilitado, isRequired = true) { estado = it.uppercase().filter(Char::isLetter).take(2) }
            }
            FormTextField("Telefone", "Telefone com DDD", value = telefone, enabled = habilitado, isRequired = true, keyboardType = KeyboardType.Phone,
                mascara = if (telefone.length > 10) "(##) #####-####" else "(##) ####-####", limiteCaracteresMascara = 11) { telefone = it }
            FormTextField("E-mail de contato", "Opcional", value = email, enabled = habilitado, keyboardType = KeyboardType.Email) { email = it.take(100) }
            Text("O CNPJ cadastrado é mantido pela instituição. O e-mail de contato é diferente do e-mail usado para entrar na conta.", fontSize = 12.sp, color = CinzaOriginal)
        }
        if (conferir) Text("Consulte novamente para conferir se a alteração foi salva antes de tentar outra edição.", color = VermelhoOriginal)
        BotaoOriginal("Atualizar dados", enabled = !ocupado, secundario = true) { atualizar++ }
    }
}