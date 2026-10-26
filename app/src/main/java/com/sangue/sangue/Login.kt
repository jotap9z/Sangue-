package com.sangue.sangue

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

private val VermelhoAuth = Color(0xFFE21C2C)
private val CinzaAuth = Color(0xFF64748B)

private enum class EtapaAuthReal {
    LOGIN,
    DOADOR_DADOS,
    DOADOR_TIPO,
    HEMO_DADOS,
    HEMO_RESPONSAVEL,
    HEMO_REVISAO,
    PROCESSANDO,
    FALHA_ACESSO
}

private data class DadosCadastroDoador(
    val nome: String = "",
    val cpf: String = "",
    val email: String = "",
    val telefone: String = "",
    val cidade: String = "",
    val nascimento: String = "",
    val senha: String = "",
    val confirmarSenha: String = "",
    val tipoSanguineo: String = "Nao_Sei",
    val termos: Boolean = false
)

private data class DadosCadastroInstituicao(
    val nome: String = "",
    val cnpj: String = "",
    val cnes: String = "",
    val telefone: String = "",
    val emailContato: String = "",
    val endereco: String = "",
    val cidade: String = "",
    val estado: String = ""
)

private data class DadosCadastroResponsavel(
    val nome: String = "",
    val email: String = "",
    val telefone: String = "",
    val senha: String = "",
    val confirmarSenha: String = "",
    val autorizado: Boolean = false
)

// Mantido somente em memória enquanto o acesso automático é concluído.
// Não é salvo em arquivo ou rememberSaveable.
private data class ContaCriadaAuth(
    val id: Long,
    val email: String,
    val senha: String,
    val hemocentro: Boolean
)

@Composable
fun AuthScreen(
    onLoginSuccess: (Boolean) -> Unit = {}
) {
    val scope = rememberCoroutineScope()

    var etapa by remember { mutableStateOf(EtapaAuthReal.LOGIN) }
    var emailLogin by remember { mutableStateOf("") }
    var senhaLogin by remember { mutableStateOf("") }

    var doador by remember {
        mutableStateOf(DadosCadastroDoador())
    }
    var instituicao by remember {
        mutableStateOf(DadosCadastroInstituicao())
    }
    var responsavel by remember {
        mutableStateOf(DadosCadastroResponsavel())
    }

    var contaCriada by remember {
        mutableStateOf<ContaCriadaAuth?>(null)
    }

    var ocupado by remember { mutableStateOf(false) }
    var erro by remember { mutableStateOf<String?>(null) }
    var cadastroIncerto by remember { mutableStateOf(false) }
    var emailConferencia by remember { mutableStateOf("") }
    var mensagemProcessamento by remember {
        mutableStateOf("Criando sua conta...")
    }
    var mostrarRecuperacao by remember {
        mutableStateOf(false)
    }

    fun retornarLogin(email: String = "") {
        if (ocupado) return

        if (email.isNotBlank()) {
            emailLogin = email
        }

        senhaLogin = ""
        doador = DadosCadastroDoador()
        instituicao = DadosCadastroInstituicao()
        responsavel = DadosCadastroResponsavel()
        contaCriada = null
        erro = null
        cadastroIncerto = false
        etapa = EtapaAuthReal.LOGIN
    }

    fun voltar() {
        if (ocupado || etapa == EtapaAuthReal.PROCESSANDO) return

        erro = null
        cadastroIncerto = false

        when (etapa) {
            EtapaAuthReal.LOGIN -> Unit
            EtapaAuthReal.DOADOR_DADOS -> retornarLogin()

            EtapaAuthReal.DOADOR_TIPO ->
                etapa = EtapaAuthReal.DOADOR_DADOS

            EtapaAuthReal.HEMO_DADOS -> retornarLogin()

            EtapaAuthReal.HEMO_RESPONSAVEL ->
                etapa = EtapaAuthReal.HEMO_DADOS

            EtapaAuthReal.HEMO_REVISAO ->
                etapa = EtapaAuthReal.HEMO_RESPONSAVEL

            EtapaAuthReal.FALHA_ACESSO ->
                retornarLogin(contaCriada?.email.orEmpty())

            EtapaAuthReal.PROCESSANDO -> Unit
        }
    }

    BackHandler(
        enabled = ocupado || etapa != EtapaAuthReal.LOGIN
    ) {
        voltar()
    }

    fun entrar() {
        if (ocupado) return

        if (emailLogin.isBlank() || senhaLogin.isBlank()) {
            erro = "Preencha o e-mail e a senha."
            return
        }

        val email = emailLogin.trim()
        val senha = senhaLogin

        ocupado = true
        erro = null

        scope.launch {
            var destino: Boolean? = null

            try {
                val usuario = SangueApi.entrar(email, senha)

                destino = when (usuario.optString("tipoUsuario")) {
                    "doador" -> false
                    "hemocentro" -> true

                    else -> {
                        SangueApi.limparSessao()
                        erro = "Perfil de acesso inválido."
                        null
                    }
                }

                if (destino != null) {
                    senhaLogin = ""
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: ApiException) {
                erro = exception.message ?: "Não foi possível entrar."
            } catch (_: Exception) {
                SangueApi.limparSessao()
                erro = "Não foi possível concluir o login."
            } finally {
                ocupado = false
            }

            destino?.let {
                onLoginSuccess(it)
            }
        }
    }

    fun acessarContaCriada() {
        if (ocupado) return

        val conta = contaCriada ?: return

        ocupado = true
        erro = null
        cadastroIncerto = false
        mensagemProcessamento = if (conta.hemocentro) {
            "Abrindo o painel do hemocentro..."
        } else {
            "Abrindo sua Home..."
        }
        etapa = EtapaAuthReal.PROCESSANDO

        scope.launch {
            var acessoConfirmado = false

            try {
                val usuario = SangueApi.entrar(
                    email = conta.email,
                    senha = conta.senha
                )

                val perfilEsperado = if (conta.hemocentro) {
                    "hemocentro"
                } else {
                    "doador"
                }

                if (
                    usuario.getLong("id") != conta.id ||
                    usuario.getString("tipoUsuario") != perfilEsperado ||
                    !usuario.getString("email")
                        .equals(conta.email, ignoreCase = true)
                ) {
                    SangueApi.limparSessao()
                    error("A conta autenticada não corresponde ao cadastro.")
                }

                contaCriada = null
                senhaLogin = ""
                acessoConfirmado = true
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: ApiException) {
                erro = "Sua conta foi criada, mas não foi possível " +
                        "abrir o painel.\n\n" +
                        (
                                exception.message
                                    ?: "Tente entrar novamente."
                                )

                etapa = EtapaAuthReal.FALHA_ACESSO
            } catch (_: Exception) {
                SangueApi.limparSessao()

                erro = "Sua conta foi criada, mas não foi possível " +
                        "abrir o painel. Tente entrar novamente."

                etapa = EtapaAuthReal.FALHA_ACESSO
            } finally {
                ocupado = false
            }

            if (acessoConfirmado) {
                onLoginSuccess(conta.hemocentro)
            }
        }
    }

    fun cadastrar(hemocentro: Boolean) {
        if (ocupado) return

        val dadosDoador = doador
        val dadosInstituicao = instituicao
        val dadosResponsavel = responsavel

        val erroValidacao = if (hemocentro) {
            validarInstituicaoCadastro(dadosInstituicao)
                ?: validarResponsavelCadastro(dadosResponsavel)
        } else {
            validarDoadorCadastro(dadosDoador)
        }

        if (erroValidacao != null) {
            erro = erroValidacao
            return
        }

        val email = if (hemocentro) {
            dadosResponsavel.email.trim()
        } else {
            dadosDoador.email.trim()
        }

        val senha = if (hemocentro) {
            dadosResponsavel.senha
        } else {
            dadosDoador.senha
        }

        val perfilEsperado = if (hemocentro) {
            "hemocentro"
        } else {
            "doador"
        }

        val etapaAnterior = if (hemocentro) {
            EtapaAuthReal.HEMO_REVISAO
        } else {
            EtapaAuthReal.DOADOR_TIPO
        }

        val corpo = if (hemocentro) {
            JSONObject()
                .put("nome", dadosInstituicao.nome.trim())
                .put("endereco", dadosInstituicao.endereco.trim())
                .put("cidade", dadosInstituicao.cidade.trim())
                .put(
                    "estado",
                    dadosInstituicao.estado.trim()
                        .uppercase(Locale.ROOT)
                )
                .put(
                    "telefone",
                    formatarTelefoneCadastro(
                        dadosInstituicao.telefone
                    )
                )
                .put(
                    "cnpj",
                    normalizarCnpjCadastro(dadosInstituicao.cnpj)
                )
                .put("cnes", dadosInstituicao.cnes.trim())
                .put(
                    "emailContato",
                    opcionalCadastro(
                        dadosInstituicao.emailContato
                    )
                )
                .put(
                    "responsavel",
                    JSONObject()
                        .put("nome", dadosResponsavel.nome.trim())
                        .put("email", email)
                        .put(
                            "telefone",
                            formatarTelefoneCadastro(
                                dadosResponsavel.telefone
                            )
                        )
                        .put("senha", senha)
                )
        } else {
            JSONObject()
                .put("nome", dadosDoador.nome.trim())
                .put("email", email)
                .put("senha", senha)
                .put(
                    "dataNascimento",
                    requireNotNull(
                        converterNascimentoCadastro(
                            dadosDoador.nascimento
                        )
                    )
                )
                .put("tipoSanguineo", dadosDoador.tipoSanguineo)
                .put(
                    "telefone",
                    opcionalCadastro(
                        formatarTelefoneCadastro(
                            dadosDoador.telefone
                        )
                    )
                )
                .put(
                    "cidade",
                    opcionalCadastro(dadosDoador.cidade)
                )
                .put(
                    "cpf",
                    opcionalCadastro(
                        dadosDoador.cpf.filter { it in '0'..'9' }
                    )
                )
        }

        ocupado = true
        erro = null
        cadastroIncerto = false
        contaCriada = null
        emailConferencia = email
        mensagemProcessamento = "Criando sua conta..."
        etapa = EtapaAuthReal.PROCESSANDO

        scope.launch {
            var cadastroConfirmado = false

            try {
                val resposta = SangueApi.objeto(
                    metodo = "POST",
                    caminho = if (hemocentro) {
                        "/api/hemocentros"
                    } else {
                        "/api/usuarios/doador"
                    },
                    corpo = corpo,
                    autenticado = false
                )

                val usuario = if (hemocentro) {
                    val centro = resposta.getJSONObject("hemocentro")
                    val conta = resposta.getJSONObject("responsavel")

                    check(centro.getLong("id") > 0)
                    check(
                        conta.getLong("hemocentroId") ==
                                centro.getLong("id")
                    )

                    conta
                } else {
                    resposta
                }

                check(usuario.getLong("id") > 0)
                check(
                    usuario.getString("tipoUsuario") == perfilEsperado
                )
                check(
                    usuario.getString("email")
                        .equals(email, ignoreCase = true)
                )

                contaCriada = ContaCriadaAuth(
                    id = usuario.getLong("id"),
                    email = usuario.getString("email"),
                    senha = senha,
                    hemocentro = hemocentro
                )

                cadastroConfirmado = true

                doador = DadosCadastroDoador()
                instituicao = DadosCadastroInstituicao()
                responsavel = DadosCadastroResponsavel()
                senhaLogin = ""
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: ApiException) {
                erro = exception.message
                    ?: "Não foi possível concluir o cadastro."

                cadastroIncerto = exception.status == 0 ||
                        exception.status >= 500

                etapa = etapaAnterior
            } catch (_: Exception) {
                erro = "Não foi possível conferir a resposta do cadastro."
                cadastroIncerto = true
                etapa = etapaAnterior
            } finally {
                ocupado = false
            }

            if (cadastroConfirmado) {
                // Autentica a conta criada. Não envia novo cadastro.
                acessarContaCriada()
            }
        }
    }

    if (etapa == EtapaAuthReal.PROCESSANDO) {
        CarregamentoCadastroAuth(
            mensagem = mensagemProcessamento
        )
        return
    }

    val titulo = when (etapa) {
        EtapaAuthReal.LOGIN -> "Sangue+"
        EtapaAuthReal.DOADOR_DADOS -> "Criar conta"
        EtapaAuthReal.DOADOR_TIPO -> "Tipo sanguíneo"

        EtapaAuthReal.HEMO_DADOS,
        EtapaAuthReal.HEMO_RESPONSAVEL,
        EtapaAuthReal.HEMO_REVISAO -> "Cadastrar hemocentro"

        EtapaAuthReal.FALHA_ACESSO -> "Acessar sua conta"
        EtapaAuthReal.PROCESSANDO -> "Sangue+"
    }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = 24.dp,
                        bottom = 12.dp,
                        start = 12.dp,
                        end = 24.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (etapa != EtapaAuthReal.LOGIN) {
                    IconButton(
                        onClick = { voltar() },
                        enabled = !ocupado
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar"
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(12.dp))
                }

                Text(
                    titulo,
                    fontSize = if (etapa == EtapaAuthReal.LOGIN) 28.sp else 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (etapa == EtapaAuthReal.LOGIN) VermelhoAuth else Color(0xFF1E293B)
                )
            }
        }
    ) { paddingValues ->
        key(etapa) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (etapa) {
                    EtapaAuthReal.DOADOR_DADOS, EtapaAuthReal.DOADOR_TIPO -> PassosOriginais(if (etapa == EtapaAuthReal.DOADOR_DADOS) 1 else 2, listOf("Seus dados", "Tipo sanguíneo"))
                    EtapaAuthReal.HEMO_DADOS, EtapaAuthReal.HEMO_RESPONSAVEL, EtapaAuthReal.HEMO_REVISAO -> PassosOriginais(
                        if (etapa == EtapaAuthReal.HEMO_DADOS) 1 else if (etapa == EtapaAuthReal.HEMO_RESPONSAVEL) 2 else 3,
                        listOf("Instituição", "Responsável", "Verificação"))
                    else -> Unit
                }
                when (etapa) {
                    EtapaAuthReal.LOGIN -> {
                        TituloAuth("Entrar na sua conta")
                        Text("Acesse seus agendamentos, histórico e carteira do doador.", color = CinzaAuth, fontSize = 14.sp)
                        Spacer(Modifier.height(12.dp))
                        FormTextField("E-mail", "Seu e-mail", value = emailLogin, enabled = !ocupado, keyboardType = KeyboardType.Email, isRequired = true) { emailLogin = it }
                        FormTextField("Senha", "Sua senha", isPassword = true, value = senhaLogin, enabled = !ocupado, isRequired = true) { senhaLogin = it }
                        TextButton(onClick = { mostrarRecuperacao = true }, enabled = !ocupado, modifier = Modifier.align(Alignment.End)) {
                            Text("Esqueci minha senha", color = VermelhoAuth, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        BotaoAuth("Entrar", !ocupado) { entrar() }
                        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            HorizontalDivider(Modifier.weight(1f), color = Color(0xFFE2E8F0))
                            Text("ou", Modifier.padding(horizontal = 12.dp), color = CinzaAuth)
                            HorizontalDivider(Modifier.weight(1f), color = Color(0xFFE2E8F0))
                        }
                        BotaoSecundarioAuth("Criar conta", !ocupado) {
                            senhaLogin = ""; erro = null; etapa = EtapaAuthReal.DOADOR_DADOS
                        }
                        Spacer(Modifier.height(8.dp))
                        CartaoOriginal(destaque = true) {
                            Text("Você representa um hemocentro?", fontWeight = FontWeight.Bold, color = TextoOriginal)
                            Text("Entre com sua conta institucional ou cadastre a instituição para gerenciar equipe, estoque e agendamentos.", fontSize = 12.sp, color = CinzaAuth)
                            BotaoSecundarioAuth("Cadastrar hemocentro", !ocupado) {
                                senhaLogin = ""; erro = null; etapa = EtapaAuthReal.HEMO_DADOS
                            }
                        }
                    }

                    EtapaAuthReal.DOADOR_DADOS -> {
                        FormularioDoadorAuth(
                            dados = doador,
                            enabled = !ocupado,
                            onChange = { doador = it }
                        )

                        BotaoAuth("Continuar", !ocupado) {
                            erro = validarDoadorCadastro(doador)

                            if (erro == null) {
                                etapa = EtapaAuthReal.DOADOR_TIPO
                            }
                        }
                    }

                    EtapaAuthReal.DOADOR_TIPO -> {
                        TituloAuth("Você sabe seu tipo sanguíneo?")
                        Text("Etapa 2 de 2", color = CinzaAuth)

                        Text(
                            "Essa informação é declarada por você. " +
                                    "Se não souber, mantenha a opção “Não sei”.",
                            color = CinzaAuth
                        )

                        val tipos = listOf(
                            "A+", "A-", "B+", "B-",
                            "AB+", "AB-", "O+", "O-"
                        )

                        tipos.chunked(4).forEach { linha ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement =
                                    Arrangement.spacedBy(8.dp)
                            ) {
                                linha.forEach { tipo ->
                                    FilterChip(
                                        selected =
                                            doador.tipoSanguineo == tipo,
                                        onClick = {
                                            doador = doador.copy(
                                                tipoSanguineo = tipo
                                            )
                                        },
                                        enabled = !ocupado,
                                        label = { Text(tipo) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        FilterChip(
                            selected =
                                doador.tipoSanguineo == "Nao_Sei",
                            onClick = {
                                doador = doador.copy(
                                    tipoSanguineo = "Nao_Sei"
                                )
                            },
                            enabled = !ocupado,
                            label = {
                                Text("Não sei meu tipo sanguíneo")
                            }
                        )

                        BotaoAuth("Concluir cadastro", !ocupado) {
                            cadastrar(hemocentro = false)
                        }
                    }

                    EtapaAuthReal.HEMO_DADOS -> {
                        FormularioInstituicaoAuth(
                            dados = instituicao,
                            enabled = !ocupado,
                            onChange = { instituicao = it }
                        )

                        BotaoAuth("Continuar", !ocupado) {
                            erro = validarInstituicaoCadastro(
                                instituicao
                            )

                            if (erro == null) {
                                etapa = EtapaAuthReal.HEMO_RESPONSAVEL
                            }
                        }
                    }

                    EtapaAuthReal.HEMO_RESPONSAVEL -> {
                        FormularioResponsavelAuth(
                            dados = responsavel,
                            enabled = !ocupado,
                            onChange = { responsavel = it }
                        )

                        BotaoAuth("Revisar cadastro", !ocupado) {
                            erro = validarResponsavelCadastro(
                                responsavel
                            )

                            if (erro == null) {
                                etapa = EtapaAuthReal.HEMO_REVISAO
                            }
                        }
                    }

                    EtapaAuthReal.HEMO_REVISAO -> {
                        TituloAuth("Revise os dados")
                        Text("Etapa 3 de 3", color = CinzaAuth)

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFF8FAFC)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement =
                                    Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    instituicao.nome.trim(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                )

                                Text(instituicao.endereco.trim())

                                Text(
                                    "${instituicao.cidade.trim()} / " +
                                            instituicao.estado.trim()
                                )

                                Text(
                                    "CNPJ: " +
                                            formatarMascaraCadastro(
                                                instituicao.cnpj,
                                                "##.###.###/####-##"
                                            )
                                )

                                Text(
                                    "CNES: ${instituicao.cnes.trim()}"
                                )

                                Text(
                                    "Telefone: " +
                                            formatarTelefoneCadastro(
                                                instituicao.telefone
                                            )
                                )

                                Text(
                                    "E-mail de contato: " +
                                            instituicao.emailContato.trim()
                                                .ifBlank { "Não informado" }
                                )

                                HorizontalDivider()

                                Text(
                                    "Responsável: " +
                                            responsavel.nome.trim()
                                )

                                Text(
                                    "E-mail de acesso: " +
                                            responsavel.email.trim()
                                )

                                Text(
                                    "Telefone do responsável: " +
                                            formatarTelefoneCadastro(
                                                responsavel.telefone
                                            )
                                )
                            }
                        }

                        Text(
                            "Ao concluir, você será direcionado " +
                                    "ao painel do hemocentro.",
                            color = CinzaAuth
                        )

                        BotaoAuth(
                            "Concluir cadastro do hemocentro",
                            !ocupado
                        ) {
                            cadastrar(hemocentro = true)
                        }
                    }

                    EtapaAuthReal.FALHA_ACESSO -> {
                        TituloAuth("Não foi possível abrir o painel")

                        Text(
                            "A conta já foi criada. Você pode tentar " +
                                    "entrar novamente sem repetir o cadastro.",
                            color = CinzaAuth
                        )

                        Text(
                            "E-mail: ${contaCriada?.email.orEmpty()}"
                        )

                        BotaoAuth(
                            "Tentar entrar novamente",
                            !ocupado && contaCriada != null
                        ) {
                            acessarContaCriada()
                        }

                        BotaoSecundarioAuth(
                            "Informar e-mail e senha",
                            !ocupado
                        ) {
                            retornarLogin(
                                contaCriada?.email.orEmpty()
                            )
                        }
                    }

                    EtapaAuthReal.PROCESSANDO -> Unit
                }

                if (ocupado) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = VermelhoAuth
                        )
                    }
                }

                erro?.let {
                    Text(
                        text = it,
                        color = VermelhoAuth,
                        fontSize = 14.sp
                    )
                }

                if (cadastroIncerto) {
                    Text(
                        "O cadastro pode ter sido concluído mesmo " +
                                "sem a resposta chegar ao aplicativo. " +
                                "Tente entrar antes de enviar novamente.",
                        color = CinzaAuth,
                        fontSize = 14.sp
                    )

                    BotaoSecundarioAuth(
                        "Conferir pelo login",
                        !ocupado
                    ) {
                        retornarLogin(emailConferencia)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (mostrarRecuperacao) {
        RecuperacaoRealDialog { mostrarRecuperacao = false }
    }
}

@Composable
private fun CarregamentoCadastroAuth(
    mensagem: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .safeDrawingPadding()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                "SANGUE+",
                color = VermelhoAuth,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold
            )

            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = VermelhoAuth
            )

            Text(
                mensagem,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )

            Text(
                "Aguarde um instante.",
                color = CinzaAuth,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun FormularioDoadorAuth(
    dados: DadosCadastroDoador,
    enabled: Boolean,
    onChange: (DadosCadastroDoador) -> Unit
) {
    TituloAuth("Seus dados básicos")
    Text("Etapa 1 de 2", color = CinzaAuth)

    FormTextField(
        "Nome completo", "Seu nome",
        isRequired = true,
        value = dados.nome,
        enabled = enabled
    ) {
        onChange(dados.copy(nome = it.take(100)))
    }

    FormTextField(
        "Data de nascimento", "dd/MM/aaaa",
        isRequired = true,
        value = dados.nascimento,
        enabled = enabled,
        keyboardType = KeyboardType.Number,
        mascara = "##/##/####"
    ) {
        onChange(dados.copy(nascimento = it))
    }

    FormTextField(
        "CPF — opcional", "000.000.000-00",
        value = dados.cpf,
        enabled = enabled,
        keyboardType = KeyboardType.Number,
        mascara = "###.###.###-##"
    ) {
        onChange(dados.copy(cpf = it))
    }

    FormTextField(
        "E-mail", "Seu e-mail de acesso",
        isRequired = true,
        value = dados.email,
        enabled = enabled,
        keyboardType = KeyboardType.Email
    ) {
        onChange(dados.copy(email = it.take(100)))
    }

    CampoTelefoneAuth(
        label = "Telefone — opcional",
        valor = dados.telefone,
        obrigatorio = false,
        enabled = enabled
    ) {
        onChange(dados.copy(telefone = it))
    }

    FormTextField(
        "Cidade — opcional", "Sua cidade",
        value = dados.cidade,
        enabled = enabled
    ) {
        onChange(dados.copy(cidade = it.take(100)))
    }

    FormTextField(
        "Senha", "Crie uma senha",
        isPassword = true,
        isRequired = true,
        value = dados.senha,
        enabled = enabled
    ) {
        onChange(dados.copy(senha = it))
    }

    FormTextField(
        "Confirmar senha", "Repita a senha",
        isPassword = true,
        isRequired = true,
        value = dados.confirmarSenha,
        enabled = enabled
    ) {
        onChange(dados.copy(confirmarSenha = it))
    }

    RegrasSenhaAuth()

    ConfirmacaoAuth(
        texto = "Concordo com os Termos e a Política de Privacidade",
        checked = dados.termos,
        enabled = enabled
    ) {
        onChange(dados.copy(termos = it))
    }
}

@Composable
private fun FormularioInstituicaoAuth(
    dados: DadosCadastroInstituicao,
    enabled: Boolean,
    onChange: (DadosCadastroInstituicao) -> Unit
) {
    TituloAuth("Dados da instituição")
    Text("Etapa 1 de 3", color = CinzaAuth)

    Text(
        "Somente o e-mail de contato é opcional.",
        color = CinzaAuth
    )

    FormTextField(
        "Nome do hemocentro", "Nome da instituição",
        isRequired = true,
        value = dados.nome,
        enabled = enabled
    ) {
        onChange(dados.copy(nome = it.take(150)))
    }

    FormTextField(
        "Endereço", "Logradouro, número e complemento",
        isRequired = true,
        value = dados.endereco,
        enabled = enabled
    ) {
        onChange(dados.copy(endereco = it.take(255)))
    }

    FormTextField(
        "Cidade", "Cidade do hemocentro",
        isRequired = true,
        value = dados.cidade,
        enabled = enabled
    ) {
        onChange(dados.copy(cidade = it.take(100)))
    }

    FormTextField(
        "Estado — UF", "Ex.: MA",
        isRequired = true,
        value = dados.estado,
        enabled = enabled
    ) {
        onChange(
            dados.copy(
                estado = it.uppercase(Locale.ROOT).take(2)
            )
        )
    }

    CampoTelefoneAuth(
        label = "Telefone institucional",
        valor = dados.telefone,
        obrigatorio = true,
        enabled = enabled
    ) {
        onChange(dados.copy(telefone = it))
    }

    FormTextField(
        "E-mail de contato — opcional",
        "Contato da instituição",
        value = dados.emailContato,
        enabled = enabled,
        keyboardType = KeyboardType.Email
    ) {
        onChange(dados.copy(emailContato = it.take(100)))
    }

    FormTextField(
        "CNPJ", "00.000.000/0000-00",
        isRequired = true,
        value = dados.cnpj,
        enabled = enabled,
        mascara = "##.###.###/####-##",
        aceitarLetrasNaMascara = true
    ) {
        onChange(dados.copy(cnpj = it))
    }

    FormTextField(
        "CNES", "7 dígitos",
        isRequired = true,
        value = dados.cnes,
        enabled = enabled,
        keyboardType = KeyboardType.Number
    ) {
        onChange(
            dados.copy(
                cnes = it.filter { caractere ->
                    caractere in '0'..'9'
                }.take(7)
            )
        )
    }
}

@Composable
private fun FormularioResponsavelAuth(
    dados: DadosCadastroResponsavel,
    enabled: Boolean,
    onChange: (DadosCadastroResponsavel) -> Unit
) {
    TituloAuth("Responsável pela conta")
    Text("Etapa 2 de 3", color = CinzaAuth)

    Text(
        "Todos os campos desta etapa são obrigatórios. " +
                "O e-mail e a senha serão usados para acessar o hemocentro.",
        color = CinzaAuth
    )

    FormTextField(
        "Nome do responsável", "Nome completo",
        isRequired = true,
        value = dados.nome,
        enabled = enabled
    ) {
        onChange(dados.copy(nome = it.take(100)))
    }

    FormTextField(
        "E-mail de acesso", "E-mail do responsável",
        isRequired = true,
        value = dados.email,
        enabled = enabled,
        keyboardType = KeyboardType.Email
    ) {
        onChange(dados.copy(email = it.take(100)))
    }

    CampoTelefoneAuth(
        label = "Telefone do responsável",
        valor = dados.telefone,
        obrigatorio = true,
        enabled = enabled
    ) {
        onChange(dados.copy(telefone = it))
    }

    FormTextField(
        "Senha", "Crie uma senha",
        isPassword = true,
        isRequired = true,
        value = dados.senha,
        enabled = enabled
    ) {
        onChange(dados.copy(senha = it))
    }

    FormTextField(
        "Confirmar senha", "Repita a senha",
        isPassword = true,
        isRequired = true,
        value = dados.confirmarSenha,
        enabled = enabled
    ) {
        onChange(dados.copy(confirmarSenha = it))
    }

    RegrasSenhaAuth()

    ConfirmacaoAuth(
        texto = "Confirmo que estou autorizado(a) a representar a instituição",
        checked = dados.autorizado,
        enabled = enabled
    ) {
        onChange(dados.copy(autorizado = it))
    }
}

@Composable
private fun CampoTelefoneAuth(
    label: String,
    valor: String,
    obrigatorio: Boolean,
    enabled: Boolean,
    onChange: (String) -> Unit
) {
    val quantidade = valor.count { it in '0'..'9' }

    FormTextField(
        label = label,
        placeholder = "(00) 00000-0000",
        isRequired = obrigatorio,
        value = valor,
        enabled = enabled,
        keyboardType = KeyboardType.Phone,
        mascara = if (quantidade > 10) {
            "(##) #####-####"
        } else {
            "(##) ####-####"
        },
        limiteCaracteresMascara = 11
    ) {
        onChange(it)
    }
}

@Composable
private fun TituloAuth(texto: String) {
    Text(
        texto,
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF1E293B)
    )
}

@Composable
private fun BotaoAuth(
    texto: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 54.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = VermelhoAuth
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(texto, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun BotaoSecundarioAuth(
    texto: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 54.dp),
        shape = RoundedCornerShape(16.dp)
        , colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = VermelhoOriginal)) {
        Text(
            texto,
            color = VermelhoAuth,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ConfirmacaoAuth(
    texto: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = CheckboxDefaults.colors(
                checkedColor = VermelhoAuth
            )
        )

        Text(
            texto,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun RegrasSenhaAuth() {
    Text(
        "A senha precisa ter pelo menos 6 caracteres, " +
                "uma letra maiúscula e um caractere especial. " +
                "O limite é de 72 bytes.",
        color = CinzaAuth,
        fontSize = 13.sp
    )
}

// Mantém compatibilidade com Perfil.kt.
@Composable
fun FormTextField(
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    isRequired: Boolean = false,
    value: String = "",
    enabled: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    mascara: String? = null,
    aceitarLetrasNaMascara: Boolean = false,
    limiteCaracteresMascara: Int? = null,
    onValueChange: (String) -> Unit = {}
) {
    var passwordVisible by remember {
        mutableStateOf(false)
    }

    val transformacaoMascara = remember(mascara) {
        mascara?.let { MascaraCadastro(it) }
    }

    val transformacao: VisualTransformation = when {
        isPassword && !passwordVisible ->
            PasswordVisualTransformation()

        transformacaoMascara != null ->
            transformacaoMascara

        else -> VisualTransformation.None
    }

    Column(
        modifier = modifier.padding(bottom = 8.dp)
    ) {
        Row {
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CinzaAuth)
            if (isRequired) Text(" *", color = VermelhoAuth, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = { novoValor ->
                if (mascara == null) {
                    onValueChange(novoValor)
                } else {
                    val limite = limiteCaracteresMascara
                        ?: mascara.count { it == '#' }

                    val filtrado = if (aceitarLetrasNaMascara) {
                        novoValor.uppercase(Locale.ROOT).filter {
                            it in '0'..'9' || it in 'A'..'Z'
                        }
                    } else {
                        novoValor.filter { it in '0'..'9' }
                    }

                    onValueChange(filtrado.take(limite))
                }
            },
            enabled = enabled,
            placeholder = {
                Text(placeholder)
            },
            visualTransformation = transformacao,
            keyboardOptions = KeyboardOptions(
                keyboardType = if (isPassword) {
                    KeyboardType.Password
                } else {
                    keyboardType
                }
            ),
            trailingIcon = if (isPassword) {
                {
                    TextButton(
                        onClick = {
                            passwordVisible = !passwordVisible
                        },
                        enabled = enabled
                    ) {
                        Text(
                            if (passwordVisible) "Ocultar" else "Mostrar",
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                null
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = VermelhoAuth,
                unfocusedBorderColor = Color(0xFFE2E8F0)
            )
        )
    }
}

private class MascaraCadastro(
    private val padrao: String
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val original = text.text
        val formatado = formatarMascaraCadastro(original, padrao)

        val originalParaFormatado = IntArray(original.length + 1)
        val formatadoParaOriginal = IntArray(formatado.length + 1)

        var quantidade = 0

        formatado.forEachIndexed { indice, caractere ->
            if (caractere.isLetterOrDigit()) {
                quantidade++

                if (quantidade < originalParaFormatado.size) {
                    originalParaFormatado[quantidade] = indice + 1
                }
            }

            formatadoParaOriginal[indice + 1] =
                quantidade.coerceAtMost(original.length)
        }

        if (quantidade < original.length) {
            for (indice in quantidade + 1..original.length) {
                originalParaFormatado[indice] = formatado.length
            }
        }

        return TransformedText(
            text = AnnotatedString(formatado),
            offsetMapping = object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int {
                    return originalParaFormatado[
                        offset.coerceIn(0, original.length)
                    ]
                }

                override fun transformedToOriginal(offset: Int): Int {
                    return formatadoParaOriginal[
                        offset.coerceIn(0, formatado.length)
                    ]
                }
            }
        )
    }
}

private fun formatarMascaraCadastro(
    valor: String,
    padrao: String
): String {
    val caracteres = valor.filter {
        it in '0'..'9' || it in 'A'..'Z' || it in 'a'..'z'
    }.take(padrao.count { it == '#' })

    return buildString {
        var indicePadrao = 0

        for (caractere in caracteres) {
            while (
                indicePadrao < padrao.length &&
                padrao[indicePadrao] != '#'
            ) {
                append(padrao[indicePadrao])
                indicePadrao++
            }

            if (indicePadrao >= padrao.length) break

            append(caractere)
            indicePadrao++
        }
    }
}

private fun formatarTelefoneCadastro(valor: String): String {
    val digitos = valor.filter { it in '0'..'9' }

    if (digitos.isEmpty()) return ""

    return formatarMascaraCadastro(
        digitos,
        if (digitos.length > 10) {
            "(##) #####-####"
        } else {
            "(##) ####-####"
        }
    )
}

private fun telefoneValidoCadastro(
    valor: String,
    obrigatorio: Boolean
): Boolean {
    if (valor.isBlank()) return !obrigatorio

    val digitos = valor.filter { it in '0'..'9' }

    return digitos.length == 10 || digitos.length == 11
}

private fun opcionalCadastro(valor: String): Any {
    return valor.trim().ifBlank { null } ?: JSONObject.NULL
}

private fun emailValidoCadastro(valor: String): Boolean {
    val email = valor.trim()

    return email.length <= 100 &&
            Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
                .matches(email)
}

private fun erroSenhaCadastro(
    senha: String,
    confirmacao: String
): String? {
    return when {
        senha != confirmacao ->
            "As senhas não coincidem."

        senha.length < 6 ->
            "A senha deve ter pelo menos 6 caracteres."

        senha.toByteArray(Charsets.UTF_8).size > 72 ->
            "A senha ultrapassa o limite de 72 bytes."

        !senha.any { it.isUpperCase() } ->
            "Inclua uma letra maiúscula na senha."

        !senha.any {
            !it.isLetterOrDigit() && !it.isWhitespace()
        } ->
            "Inclua um caractere especial na senha."

        else -> null
    }
}

private fun validarDoadorCadastro(
    dados: DadosCadastroDoador
): String? {
    if (dados.nome.trim().length !in 2..100) {
        return "Informe um nome entre 2 e 100 caracteres."
    }

    if (!emailValidoCadastro(dados.email)) {
        return "Informe um e-mail válido."
    }

    val nascimento = converterNascimentoCadastro(
        dados.nascimento
    ) ?: return "Informe uma data de nascimento válida: dd/MM/aaaa."

    if (idadeCadastro(nascimento) < 16) {
        return "O cadastro de doador está disponível a partir de 16 anos."
    }

    if (!cpfValidoCadastro(dados.cpf)) {
        return "Informe um CPF válido ou deixe o campo vazio."
    }

    if (
        dados.cidade.isNotBlank() &&
        dados.cidade.trim().length !in 2..100
    ) {
        return "A cidade deve ter entre 2 e 100 caracteres."
    }

    if (
        !telefoneValidoCadastro(
            dados.telefone,
            obrigatorio = false
        )
    ) {
        return "Informe o telefone com DDD e 10 ou 11 dígitos."
    }

    erroSenhaCadastro(
        dados.senha,
        dados.confirmarSenha
    )?.let {
        return it
    }

    if (!dados.termos) {
        return "Confirme a concordância com os Termos e a Política."
    }

    if (
        dados.tipoSanguineo !in setOf(
            "A+", "A-", "B+", "B-",
            "AB+", "AB-", "O+", "O-", "Nao_Sei"
        )
    ) {
        return "Selecione um tipo sanguíneo válido ou “Não sei”."
    }

    return null
}

private fun validarInstituicaoCadastro(
    dados: DadosCadastroInstituicao
): String? {
    if (dados.nome.trim().length !in 2..150) {
        return "O nome do hemocentro deve ter entre 2 e 150 caracteres."
    }

    if (dados.endereco.trim().length !in 5..255) {
        return "O endereço deve ter entre 5 e 255 caracteres."
    }

    if (dados.cidade.trim().length !in 2..100) {
        return "A cidade deve ter entre 2 e 100 caracteres."
    }

    val estados = setOf(
        "AC", "AL", "AP", "AM", "BA", "CE", "DF",
        "ES", "GO", "MA", "MT", "MS", "MG", "PA",
        "PB", "PR", "PE", "PI", "RJ", "RN", "RS",
        "RO", "RR", "SC", "SP", "SE", "TO"
    )

    if (
        dados.estado.trim().uppercase(Locale.ROOT) !in estados
    ) {
        return "Informe uma UF válida, como MA."
    }

    if (
        !telefoneValidoCadastro(
            dados.telefone,
            obrigatorio = true
        )
    ) {
        return "Informe o telefone institucional com DDD e 10 ou 11 dígitos."
    }

    if (
        dados.emailContato.isNotBlank() &&
        !emailValidoCadastro(dados.emailContato)
    ) {
        return "Informe um e-mail de contato válido ou deixe vazio."
    }

    val cnpj = normalizarCnpjCadastro(dados.cnpj)

    if (!Regex("^[A-Z0-9]{14}$").matches(cnpj)) {
        return "Informe o CNPJ com 14 caracteres, sem contar a pontuação."
    }

    if (!Regex("^[0-9]{7}$").matches(dados.cnes.trim())) {
        return "Informe o CNES com 7 dígitos."
    }

    return null
}

private fun validarResponsavelCadastro(
    dados: DadosCadastroResponsavel
): String? {
    if (dados.nome.trim().length !in 2..100) {
        return "Informe um nome entre 2 e 100 caracteres."
    }

    if (!emailValidoCadastro(dados.email)) {
        return "Informe um e-mail de acesso válido."
    }

    if (
        !telefoneValidoCadastro(
            dados.telefone,
            obrigatorio = true
        )
    ) {
        return "Informe o telefone do responsável com DDD e 10 ou 11 dígitos."
    }

    erroSenhaCadastro(
        dados.senha,
        dados.confirmarSenha
    )?.let {
        return it
    }

    if (!dados.autorizado) {
        return "Confirme que está autorizado(a) a representar a instituição."
    }

    return null
}

private fun normalizarCnpjCadastro(valor: String): String {
    return valor.trim()
        .replace(Regex("[.\\-/\\s]"), "")
        .uppercase(Locale.ROOT)
}

private fun cpfValidoCadastro(valor: String): Boolean {
    if (valor.isBlank()) return true

    if (!Regex("^[0-9.\\-\\s]+$").matches(valor.trim())) {
        return false
    }

    val cpf = valor.filter { it in '0'..'9' }

    if (cpf.length != 11 || cpf.toSet().size == 1) {
        return false
    }

    fun digito(tamanho: Int): Int {
        val soma = (0 until tamanho).sumOf { indice ->
            (cpf[indice] - '0') * (tamanho + 1 - indice)
        }

        val resto = soma % 11
        return if (resto < 2) 0 else 11 - resto
    }

    return cpf[9] - '0' == digito(9) &&
            cpf[10] - '0' == digito(10)
}

private fun converterNascimentoCadastro(valor: String): String? {
    val entrada = valor.trim()

    val texto = if (Regex("^\\d{8}$").matches(entrada)) {
        formatarMascaraCadastro(entrada, "##/##/####")
    } else {
        entrada
    }

    if (!Regex("^\\d{2}/\\d{2}/\\d{4}$").matches(texto)) {
        return null
    }

    val formato = SimpleDateFormat(
        "dd/MM/yyyy",
        Locale.ROOT
    ).apply {
        isLenient = false
        timeZone = TimeZone.getTimeZone("UTC")
    }

    val posicao = ParsePosition(0)
    val data = formato.parse(texto, posicao) ?: return null

    if (
        posicao.index != texto.length ||
        formato.format(data) != texto
    ) {
        return null
    }

    return SimpleDateFormat(
        "yyyy-MM-dd",
        Locale.ROOT
    ).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }.format(data)
}

private fun idadeCadastro(nascimentoIso: String): Int {
    val partes = nascimentoIso.split("-")
    val ano = partes[0].toInt()
    val mes = partes[1].toInt()
    val dia = partes[2].toInt()

    val hoje = Calendar.getInstance(
        TimeZone.getTimeZone("America/Sao_Paulo")
    )

    var idade = hoje.get(Calendar.YEAR) - ano
    val mesAtual = hoje.get(Calendar.MONTH) + 1
    val diaAtual = hoje.get(Calendar.DAY_OF_MONTH)

    if (
        mesAtual < mes ||
        (mesAtual == mes && diaAtual < dia)
    ) {
        idade--
    }

    return idade
}