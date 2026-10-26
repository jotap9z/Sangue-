package com.sangue.sangue

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.json.JSONObject

private val VermelhoEquipe = Color(0xFFE21C2C)
private val CinzaEquipe = Color(0xFF64748B)

private data class ConviteGeradoEquipe(
    val convite: JSONObject,
    val codigo: String
)

@Composable
fun HemoEquipeScreen(
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val usuarioId = SangueApi.usuario?.optLong("id")
    val hemocentroId = SangueApi.usuario?.let {
        if (it.isNull("hemocentroId")) null
        else it.optLong("hemocentroId")
    }

    var meuVinculo by remember(usuarioId) {
        mutableStateOf<JSONObject?>(null)
    }
    var membros by remember(usuarioId) {
        mutableStateOf<List<JSONObject>>(emptyList())
    }
    var convites by remember(usuarioId) {
        mutableStateOf<List<JSONObject>>(emptyList())
    }
    var conviteGerado by remember(usuarioId) {
        mutableStateOf<ConviteGeradoEquipe?>(null)
    }
    var carregando by remember(usuarioId) {
        mutableStateOf(true)
    }
    var processando by remember(usuarioId) {
        mutableStateOf(false)
    }
    var conferido by remember(usuarioId) {
        mutableStateOf(false)
    }
    var permiteInicializar by remember(usuarioId) {
        mutableStateOf(false)
    }
    var erroConsulta by remember(usuarioId) {
        mutableStateOf<String?>(null)
    }
    var erroOperacao by remember(usuarioId) {
        mutableStateOf<String?>(null)
    }
    var mensagem by remember(usuarioId) {
        mutableStateOf<String?>(null)
    }
    var atualizar by remember(usuarioId) {
        mutableStateOf(0)
    }

    var mostrarFormulario by remember {
        mutableStateOf(false)
    }
    var nome by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var telefone by remember { mutableStateOf("") }
    var cargo by remember { mutableStateOf("") }
    var funcao by remember { mutableStateOf("ATENDIMENTO") }
    var permissao by remember { mutableStateOf("PADRAO") }

    var confirmarInicializacao by remember {
        mutableStateOf(false)
    }
    var mostrarAtivacao by remember { mutableStateOf(false) }
    var senhaAtivacao by remember { mutableStateOf("") }
    var confirmarSenhaAtivacao by remember { mutableStateOf("") }
    var erroDialogo by remember { mutableStateOf<String?>(null) }

    var membroEdicao by remember {
        mutableStateOf<JSONObject?>(null)
    }
    var funcaoEdicao by remember { mutableStateOf("ATENDIMENTO") }
    var permissaoEdicao by remember { mutableStateOf("PADRAO") }
    var cargoEdicao by remember { mutableStateOf("") }

    var membroStatus by remember {
        mutableStateOf<JSONObject?>(null)
    }
    var conviteCancelar by remember {
        mutableStateOf<JSONObject?>(null)
    }

    var mostrarTransferencia by remember { mutableStateOf(false) }
    var novoPrincipal by remember { mutableStateOf("") }
    var senhaTransferencia by remember { mutableStateOf("") }

    val ocupado = carregando || processando
    val administrador =
        meuVinculo?.optString("permissao") == "ADMINISTRADOR"
    val principal =
        meuVinculo?.optBoolean("administradorPrincipal") == true

    suspend fun carregarEquipe() {
        carregando = true
        conferido = false
        permiteInicializar = false
        erroConsulta = null
        meuVinculo = null
        membros = emptyList()
        convites = emptyList()

        try {
            if (!SangueApi.autenticado) {
                erroConsulta = "Entre com uma conta de hemocentro."
                return
            }

            val vinculo = try {
                SangueApi.objeto(
                    metodo = "GET",
                    caminho = "/api/hemocentros/me/equipe/me"
                )
            } catch (exception: ApiException) {
                if (exception.status == 403) {
                    permiteInicializar = true
                }
                throw exception
            }

            check(vinculo.getLong("usuarioId") == usuarioId)
            check(vinculo.getLong("hemocentroId") == hemocentroId)

            meuVinculo = vinculo

            if (vinculo.getString("permissao") == "ADMINISTRADOR") {
                val resposta = SangueApi.objeto(
                    metodo = "GET",
                    caminho = "/api/hemocentros/me/equipe"
                )

                val listaMembros = resposta.getJSONArray("membros")
                val listaConvites = resposta.getJSONArray("convites")

                val novosMembros = (0 until listaMembros.length()).map {
                    listaMembros.getJSONObject(it)
                }
                val novosConvites = (0 until listaConvites.length()).map {
                    listaConvites.getJSONObject(it)
                }

                check(
                    novosMembros.all {
                        it.getLong("hemocentroId") == hemocentroId
                    }
                )
                check(
                    novosConvites.all {
                        it.getLong("hemocentroId") == hemocentroId
                    }
                )

                membros = novosMembros
                convites = novosConvites

                val gerado = conviteGerado
                if (gerado != null) {
                    val atual = novosConvites.firstOrNull {
                        it.getLong("id") ==
                                gerado.convite.getLong("id")
                    }

                    if (atual?.optString("status") != "PENDENTE") {
                        conviteGerado = null
                    }
                }
            }

            conferido = true
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: ApiException) {
            erroConsulta = exception.message
                ?: "Não foi possível consultar a equipe."
        } catch (_: Exception) {
            erroConsulta = "Não foi possível consultar a equipe."
        } finally {
            carregando = false
        }
    }

    LaunchedEffect(usuarioId, atualizar) {
        carregarEquipe()
    }

    fun operar(acao: suspend () -> Unit) {
        if (ocupado || !SangueApi.autenticado) return

        processando = true
        conferido = false
        erroOperacao = null
        mensagem = null

        scope.launch {
            try {
                acao()
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: ApiException) {
                erroOperacao = (
                        exception.message
                            ?: "Não foi possível confirmar a operação."
                        ) + "\nConfira a lista atualizada antes de tentar novamente."
            } catch (_: Exception) {
                erroOperacao = "Não foi possível conferir a resposta. " +
                        "Confira a lista atualizada antes de tentar novamente."
            } finally {
                try {
                    carregarEquipe()
                } finally {
                    processando = false
                }
            }
        }
    }

    BackHandler {
        if (!processando) {
            onNavigateBack()
        }
    }

    val podeGerenciar = administrador &&
            conferido &&
            !ocupado

    val candidatosPrincipal = membros.filter {
        it.getLong("usuarioId") != usuarioId &&
                it.getBoolean("ativo") &&
                it.getString("funcao") == "ADMINISTRADOR" &&
                it.getString("permissao") == "ADMINISTRADOR"
    }

    Scaffold(
        containerColor = Color.White,
        topBar = { CabecalhoOriginal("Equipe e acessos", onNavigateBack, !processando, acao = {
            IconButton(onClick = { atualizar++ }, enabled = !ocupado) { Icon(Icons.Outlined.Refresh, "Atualizar equipe", tint = VermelhoEquipe) }
        }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (ocupado) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = VermelhoEquipe
                )

                Text(
                    if (processando) {
                        "Confirmando operação..."
                    } else {
                        "Consultando equipe..."
                    },
                    color = CinzaEquipe
                )
            }

            mensagem?.let {
                Text(
                    it,
                    color = Color(0xFF15803D),
                    fontWeight = FontWeight.SemiBold
                )
            }

            erroOperacao?.let {
                Text(it, color = VermelhoEquipe)
            }

            erroConsulta?.let {
                Text(it, color = VermelhoEquipe)
            }

            if (permiteInicializar && !ocupado) {
                Text(
                    "Inicialização da equipe",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "Se este é o primeiro acesso do responsável, " +
                            "inicialize a equipe. Sua conta será cadastrada " +
                            "como administrador principal. Nenhum funcionário " +
                            "de demonstração será incluído.",
                    color = CinzaEquipe
                )

                Button(
                    onClick = {
                        confirmarInicializacao = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VermelhoEquipe
                    )
                    , shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)) {
                    Text("Inicializar equipe")
                }
            }

            meuVinculo?.let { vinculo ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF8FAFC)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Seu acesso",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(vinculo.getString("nome"))
                        Text(vinculo.getString("email"))
                        Text(
                            "Função: " +
                                    nomeFuncaoEquipe(vinculo.getString("funcao"))
                        )
                        Text(
                            "Permissão: " +
                                    nomePermissaoEquipe(
                                        vinculo.getString("permissao")
                                    )
                        )

                        if (
                            vinculo.getBoolean("administradorPrincipal")
                        ) {
                            Text(
                                "Administrador principal",
                                color = VermelhoEquipe,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            if (conferido && !administrador) {
                Text(
                    "A consulta completa e a gestão da equipe " +
                            "são exclusivas dos administradores.",
                    color = CinzaEquipe
                )
            }

            if (administrador && conferido) {
                Text(
                    "Membros cadastrados: ${membros.size}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                if (membros.isEmpty()) {
                    Text(
                        "Nenhum membro cadastrado.",
                        color = CinzaEquipe
                    )
                }

                membros.forEach { membro ->
                    val id = membro.getLong("usuarioId")
                    val ehPrincipal =
                        membro.getBoolean("administradorPrincipal")
                    val ativo = membro.getBoolean("ativo")

                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement =
                                Arrangement.spacedBy(8.dp)
                        ) {
                            AvatarOriginal(membro.getString("nome"))
                            Text(
                                membro.getString("nome"),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(membro.getString("email"))

                            textoEquipe(membro, "telefone")?.let {
                                Text(it, color = CinzaEquipe)
                            }

                            textoEquipe(membro, "cargo")?.let {
                                Text("Cargo: $it", color = CinzaEquipe)
                            }

                            Text(
                                nomeFuncaoEquipe(
                                    membro.getString("funcao")
                                ) + " · " +
                                        nomePermissaoEquipe(
                                            membro.getString("permissao")
                                        )
                            )

                            Text(
                                if (ativo) "Ativo" else "Desativado",
                                color = if (ativo) {
                                    Color(0xFF15803D)
                                } else {
                                    VermelhoEquipe
                                }
                            )

                            if (ehPrincipal) {
                                Text(
                                    "Administrador principal",
                                    color = VermelhoEquipe,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    membroEdicao = membro
                                    funcaoEdicao =
                                        membro.getString("funcao")
                                    permissaoEdicao =
                                        membro.getString("permissao")
                                    cargoEdicao =
                                        textoEquipe(membro, "cargo")
                                            .orEmpty()
                                },
                                enabled = podeGerenciar,
                                modifier = Modifier.fillMaxWidth()
                                , shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp), colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = VermelhoOriginal)) {
                                Text("Editar função e permissão")
                            }

                            if (!ehPrincipal && id != usuarioId) {
                                OutlinedButton(
                                    onClick = {
                                        membroStatus = membro
                                    },
                                    enabled = podeGerenciar,
                                    modifier = Modifier.fillMaxWidth()
                                    , shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp), colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = VermelhoOriginal)) {
                                    Text(
                                        if (ativo) {
                                            "Desativar acesso"
                                        } else {
                                            "Reativar acesso"
                                        },
                                        color = VermelhoEquipe
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider()

                Button(
                    onClick = {
                        mostrarFormulario = !mostrarFormulario
                    },
                    enabled = podeGerenciar,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VermelhoEquipe
                    )
                    , shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)) {
                    Text(
                        if (mostrarFormulario) {
                            "Fechar formulário"
                        } else {
                            "Convidar funcionário"
                        }
                    )
                }

                if (mostrarFormulario) {
                    Text(
                        "Novo funcionário",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    FormTextField(
                        "Nome", "Nome completo",
                        isRequired = true,
                        value = nome,
                        enabled = podeGerenciar
                    ) {
                        nome = it.take(100)
                    }

                    FormTextField(
                        "E-mail de acesso", "E-mail do funcionário",
                        isRequired = true,
                        value = email,
                        enabled = podeGerenciar
                    ) {
                        email = it.take(100)
                    }

                    FormTextField(
                        "Telefone — opcional", "Telefone com DDD",
                        value = telefone,
                        enabled = podeGerenciar
                    ) {
                        telefone = it.take(30)
                    }

                    FormTextField(
                        "Cargo — opcional", "Cargo na instituição",
                        value = cargo,
                        enabled = podeGerenciar
                    ) {
                        cargo = it.take(100)
                    }

                    SelecaoEquipe(
                        titulo = "Função",
                        valor = funcao,
                        opcoes = funcoesEquipe(),
                        enabled = podeGerenciar
                    ) {
                        funcao = it
                        permissao = if (it == "ADMINISTRADOR") {
                            "ADMINISTRADOR"
                        } else {
                            "PADRAO"
                        }
                    }

                    SelecaoEquipe(
                        titulo = "Permissão",
                        valor = permissao,
                        opcoes = permissoesEquipe(funcao),
                        enabled = podeGerenciar
                    ) {
                        permissao = it
                    }

                    Button(
                        onClick = {
                            if (nome.trim().length !in 2..100) {
                                erroOperacao =
                                    "Informe um nome entre 2 e 100 caracteres."
                            } else if (
                                !Regex(
                                    "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"
                                ).matches(email.trim())
                            ) {
                                erroOperacao = "Informe um e-mail válido."
                            } else {
                                val corpo = JSONObject()
                                    .put("nome", nome.trim())
                                    .put("email", email.trim())
                                    .put("funcao", funcao)
                                    .put("permissao", permissao)
                                    .put("telefone", opcionalEquipe(telefone))
                                    .put("cargo", opcionalEquipe(cargo))

                                operar {
                                    val resposta = SangueApi.objeto(
                                        metodo = "POST",
                                        caminho =
                                            "/api/hemocentros/me/equipe/convites",
                                        corpo = corpo
                                    )

                                    val convite = resposta.getJSONObject(
                                        "convite"
                                    )
                                    val codigo = resposta.getString(
                                        "codigoConvite"
                                    )

                                    check(
                                        convite.getLong("hemocentroId") ==
                                                hemocentroId
                                    )
                                    check(
                                        Regex("^[A-Za-z0-9_-]{43}$")
                                            .matches(codigo)
                                    )
                                    check(
                                        convite.getString("email").equals(
                                            corpo.getString("email"),
                                            ignoreCase = true
                                        )
                                    )

                                    conviteGerado = ConviteGeradoEquipe(
                                        convite,
                                        codigo
                                    )

                                    nome = ""
                                    email = ""
                                    telefone = ""
                                    cargo = ""
                                    funcao = "ATENDIMENTO"
                                    permissao = "PADRAO"
                                    mostrarFormulario = false

                                    mensagem = "Convite criado. " +
                                            "O funcionário ainda precisa " +
                                            "ativar a conta."
                                }
                            }
                        },
                        enabled = podeGerenciar,
                        modifier = Modifier.fillMaxWidth()
                        , shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp), colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = VermelhoOriginal, contentColor = androidx.compose.ui.graphics.Color.White)) {
                        Text("Gerar convite")
                    }
                }

                conviteGerado?.let { gerado ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFEF2F2)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement =
                                Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                "Convite gerado",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )

                            Text(
                                gerado.convite.getString("nome") +
                                        "\n" +
                                        gerado.convite.getString("email")
                            )

                            OutlinedTextField(
                                value = gerado.codigo,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Código do convite") },
                                modifier = Modifier.fillMaxWidth()
                                , shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp))

                            Text(
                                "Guarde o código. Ele só é exibido " +
                                        "nesta geração e expira em 48 horas.",
                                color = CinzaEquipe,
                                fontSize = 13.sp
                            )

                            TextButton(
                                onClick = {
                                    val clipboard =
                                        context.getSystemService(
                                            Context.CLIPBOARD_SERVICE
                                        ) as ClipboardManager

                                    clipboard.setPrimaryClip(
                                        ClipData.newPlainText(
                                            "Convite Sangue+",
                                            gerado.codigo
                                        )
                                    )

                                    mensagem = "Código copiado."
                                },
                                enabled = !ocupado
                            ) {
                                Text("Copiar código")
                            }

                            Button(
                                onClick = {
                                    senhaAtivacao = ""
                                    confirmarSenhaAtivacao = ""
                                    erroDialogo = null
                                    mostrarAtivacao = true
                                },
                                enabled = podeGerenciar,
                                modifier = Modifier.fillMaxWidth()
                                , shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp), colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = VermelhoOriginal, contentColor = androidx.compose.ui.graphics.Color.White)) {
                                Text("Ativar conta neste aparelho")
                            }
                        }
                    }
                }

                HorizontalDivider()

                Text(
                    "Convites",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                if (convites.isEmpty()) {
                    Text(
                        "Nenhum convite cadastrado.",
                        color = CinzaEquipe
                    )
                }

                convites.forEach { convite ->
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement =
                                Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                convite.getString("nome"),
                                fontWeight = FontWeight.Bold
                            )
                            Text(convite.getString("email"))
                            Text(
                                nomeFuncaoEquipe(
                                    convite.getString("funcao")
                                )
                            )
                            Text(
                                "Situação: " +
                                        nomeStatusConviteEquipe(
                                            convite.getString("status")
                                        )
                            )

                            if (
                                convite.getString("status") == "PENDENTE"
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        conviteCancelar = convite
                                    },
                                    enabled = podeGerenciar
                                    , shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp), colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = VermelhoOriginal)) {
                                    Text(
                                        "Cancelar convite",
                                        color = VermelhoEquipe
                                    )
                                }
                            }
                        }
                    }
                }

                if (principal) {
                    HorizontalDivider()

                    Text(
                        "Administrador principal",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        "A transferência exige outro membro ativo " +
                                "com função e permissão de administrador.",
                        color = CinzaEquipe
                    )

                    OutlinedButton(
                        onClick = {
                            novoPrincipal = ""
                            senhaTransferencia = ""
                            erroDialogo = null
                            mostrarTransferencia = true
                        },
                        enabled = podeGerenciar &&
                                candidatosPrincipal.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth()
                        , shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp), colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = VermelhoOriginal)) {
                        Text("Transferir administração principal")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (confirmarInicializacao) {
        AlertDialog(
            onDismissRequest = {
                confirmarInicializacao = false
            },
            title = { Text("Inicializar equipe?") },
            text = {
                Text(
                    "A conta de " +
                            SangueApi.usuario?.optString("nome").orEmpty() +
                            " será o administrador principal. A API " +
                            "verificará se existe somente uma conta " +
                            "institucional neste hemocentro."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmarInicializacao = false

                        operar {
                            val resposta = SangueApi.objeto(
                                metodo = "POST",
                                caminho =
                                    "/api/hemocentros/me/equipe/inicializar"
                            )

                            check(
                                resposta.getLong("usuarioId") == usuarioId
                            )
                            check(
                                resposta.getBoolean(
                                    "administradorPrincipal"
                                )
                            )

                            mensagem = "Equipe inicializada. " +
                                    "Você é o administrador principal."
                        }
                    },
                    enabled = !ocupado
                ) {
                    Text("Inicializar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        confirmarInicializacao = false
                    }
                ) {
                    Text("Voltar")
                }
            }
            , shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp), containerColor = androidx.compose.ui.graphics.Color.White)
    }

    val geradoAtivacao = conviteGerado

    if (mostrarAtivacao && geradoAtivacao != null) {
        AlertDialog(
            onDismissRequest = {
                mostrarAtivacao = false
                senhaAtivacao = ""
                confirmarSenhaAtivacao = ""
            },
            title = { Text("Ativar conta do funcionário") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(
                        rememberScrollState()
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        geradoAtivacao.convite.getString("nome") +
                                "\n" +
                                geradoAtivacao.convite.getString("email")
                    )

                    Text(
                        "O funcionário deve definir a senha de acesso. " +
                                "Sua conta de administrador continuará conectada."
                    )

                    FormTextField(
                        "Senha", "Senha do funcionário",
                        isPassword = true,
                        value = senhaAtivacao
                    ) {
                        senhaAtivacao = it
                    }

                    FormTextField(
                        "Confirmar senha", "Repita a senha",
                        isPassword = true,
                        value = confirmarSenhaAtivacao
                    ) {
                        confirmarSenhaAtivacao = it
                    }

                    Text(
                        "Mínimo de 6 caracteres, uma letra maiúscula " +
                                "e um caractere especial. Máximo de 72 bytes.",
                        fontSize = 13.sp
                    )

                    erroDialogo?.let {
                        Text(it, color = VermelhoEquipe)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (
                            senhaAtivacao != confirmarSenhaAtivacao ||
                            !senhaValidaEquipe(senhaAtivacao)
                        ) {
                            erroDialogo =
                                "Confira as senhas e os requisitos informados."
                        } else {
                            val senha = senhaAtivacao
                            mostrarAtivacao = false
                            senhaAtivacao = ""
                            confirmarSenhaAtivacao = ""

                            operar {
                                val resposta = SangueApi.objeto(
                                    metodo = "POST",
                                    caminho = "/api/equipe/convites/aceitar",
                                    corpo = JSONObject()
                                        .put(
                                            "codigo",
                                            geradoAtivacao.codigo
                                        )
                                        .put("senha", senha),
                                    autenticado = false
                                )

                                check(
                                    resposta.getLong("hemocentroId") ==
                                            hemocentroId
                                )
                                check(
                                    resposta.getString("email").equals(
                                        geradoAtivacao.convite.getString(
                                            "email"
                                        ),
                                        ignoreCase = true
                                    )
                                )

                                conviteGerado = null
                                mensagem = "Funcionário cadastrado. " +
                                        "Ele já pode entrar com seu próprio " +
                                        "e-mail e senha."
                            }
                        }
                    },
                    enabled = !ocupado
                ) {
                    Text("Ativar conta")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        mostrarAtivacao = false
                        senhaAtivacao = ""
                        confirmarSenhaAtivacao = ""
                    }
                ) {
                    Text("Voltar")
                }
            }
            , shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp), containerColor = androidx.compose.ui.graphics.Color.White)
    }

    val editando = membroEdicao

    if (editando != null) {
        AlertDialog(
            onDismissRequest = { membroEdicao = null },
            title = { Text("Editar acesso") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(
                        rememberScrollState()
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(editando.getString("nome"))

                    SelecaoEquipe(
                        "Função",
                        funcaoEdicao,
                        funcoesEquipe(),
                        !ocupado
                    ) {
                        funcaoEdicao = it
                        permissaoEdicao = if (it == "ADMINISTRADOR") {
                            "ADMINISTRADOR"
                        } else {
                            "PADRAO"
                        }
                    }

                    SelecaoEquipe(
                        "Permissão",
                        permissaoEdicao,
                        permissoesEquipe(funcaoEdicao),
                        !ocupado
                    ) {
                        permissaoEdicao = it
                    }

                    FormTextField(
                        "Cargo — opcional", "Cargo",
                        value = cargoEdicao,
                        enabled = !ocupado
                    ) {
                        cargoEdicao = it.take(100)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val id = editando.getLong("usuarioId")
                        val corpo = JSONObject()
                            .put("funcao", funcaoEdicao)
                            .put("permissao", permissaoEdicao)
                            .put(
                                "cargo",
                                opcionalEquipe(cargoEdicao)
                            )

                        membroEdicao = null

                        operar {
                            val resposta = SangueApi.objeto(
                                metodo = "PUT",
                                caminho = "/api/hemocentros/me/equipe/" +
                                        "$id/funcao",
                                corpo = corpo
                            )

                            check(resposta.getLong("usuarioId") == id)
                            check(
                                resposta.getString("funcao") ==
                                        corpo.getString("funcao")
                            )
                            check(
                                resposta.getString("permissao") ==
                                        corpo.getString("permissao")
                            )

                            mensagem = "Função e permissão atualizadas."
                        }
                    },
                    enabled = !ocupado
                ) {
                    Text("Salvar")
                }
            },
            dismissButton = {
                TextButton(onClick = { membroEdicao = null }) {
                    Text("Voltar")
                }
            }
            , shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp), containerColor = androidx.compose.ui.graphics.Color.White)
    }

    val alterandoStatus = membroStatus

    if (alterandoStatus != null) {
        val novoStatus = !alterandoStatus.getBoolean("ativo")

        AlertDialog(
            onDismissRequest = { membroStatus = null },
            title = {
                Text(
                    if (novoStatus) "Reativar acesso?"
                    else "Desativar acesso?"
                )
            },
            text = {
                Text(
                    alterandoStatus.getString("nome") +
                            if (!novoStatus) {
                                "\n\nAs sessões desse funcionário " +
                                        "serão encerradas."
                            } else {
                                "\n\nO funcionário poderá entrar novamente."
                            }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val id = alterandoStatus.getLong("usuarioId")
                        membroStatus = null

                        operar {
                            val resposta = SangueApi.objeto(
                                metodo = "PUT",
                                caminho = "/api/hemocentros/me/equipe/" +
                                        "$id/status",
                                corpo = JSONObject().put(
                                    "ativo",
                                    novoStatus
                                )
                            )

                            check(resposta.getLong("usuarioId") == id)
                            check(
                                resposta.getBoolean("ativo") == novoStatus
                            )

                            mensagem = "Situação do acesso atualizada."
                        }
                    },
                    enabled = !ocupado
                ) {
                    Text("Confirmar")
                }
            },
            dismissButton = {
                TextButton(onClick = { membroStatus = null }) {
                    Text("Voltar")
                }
            }
            , shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp), containerColor = androidx.compose.ui.graphics.Color.White)
    }

    val cancelandoConvite = conviteCancelar

    if (cancelandoConvite != null) {
        AlertDialog(
            onDismissRequest = { conviteCancelar = null },
            title = { Text("Cancelar convite?") },
            text = {
                Text(cancelandoConvite.getString("email"))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val id = cancelandoConvite.getLong("id")
                        conviteCancelar = null

                        operar {
                            val resposta = SangueApi.objeto(
                                metodo = "POST",
                                caminho =
                                    "/api/hemocentros/me/equipe/convites/" +
                                            "$id/cancelar"
                            )

                            check(resposta.getLong("id") == id)
                            check(
                                resposta.getString("status") == "CANCELADO"
                            )

                            mensagem = "Convite cancelado."
                        }
                    },
                    enabled = !ocupado
                ) {
                    Text("Cancelar convite")
                }
            },
            dismissButton = {
                TextButton(onClick = { conviteCancelar = null }) {
                    Text("Voltar")
                }
            }
            , shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp), containerColor = androidx.compose.ui.graphics.Color.White)
    }

    if (mostrarTransferencia) {
        AlertDialog(
            onDismissRequest = {
                mostrarTransferencia = false
                senhaTransferencia = ""
            },
            title = { Text("Transferir administração principal") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(
                        rememberScrollState()
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "O membro escolhido passará a ser o " +
                                "administrador principal. Sua conta " +
                                "continuará como administrador."
                    )

                    SelecaoEquipe(
                        titulo = "Novo administrador principal",
                        valor = novoPrincipal,
                        opcoes = candidatosPrincipal.map {
                            it.getLong("usuarioId").toString() to
                                    (
                                            it.getString("nome") +
                                                    " — " + it.getString("email")
                                            )
                        },
                        enabled = !ocupado
                    ) {
                        novoPrincipal = it
                    }

                    FormTextField(
                        "Sua senha atual", "Confirme sua senha",
                        isPassword = true,
                        value = senhaTransferencia,
                        enabled = !ocupado
                    ) {
                        senhaTransferencia = it
                    }

                    erroDialogo?.let {
                        Text(it, color = VermelhoEquipe)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val id = novoPrincipal.toLongOrNull()
                        val senha = senhaTransferencia

                        if (
                            id == null ||
                            senha.isBlank() ||
                            candidatosPrincipal.none {
                                it.getLong("usuarioId") == id
                            }
                        ) {
                            erroDialogo =
                                "Selecione um administrador e informe sua senha."
                        } else {
                            mostrarTransferencia = false
                            senhaTransferencia = ""

                            operar {
                                val resposta = SangueApi.objeto(
                                    metodo = "POST",
                                    caminho =
                                        "/api/hemocentros/me/equipe/" +
                                                "administrador-principal/transferir",
                                    corpo = JSONObject()
                                        .put(
                                            "novoAdministradorUsuarioId",
                                            id
                                        )
                                        .put("senhaAtual", senha)
                                )

                                val novo = resposta.getJSONObject(
                                    "administradorAtual"
                                )

                                check(novo.getLong("usuarioId") == id)
                                check(
                                    novo.getBoolean(
                                        "administradorPrincipal"
                                    )
                                )

                                mensagem =
                                    "Administração principal transferida."
                            }
                        }
                    },
                    enabled = !ocupado
                ) {
                    Text("Confirmar transferência")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        mostrarTransferencia = false
                        senhaTransferencia = ""
                    }
                ) {
                    Text("Voltar")
                }
            }
            , shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp), containerColor = androidx.compose.ui.graphics.Color.White)
    }
}

@Composable
private fun SelecaoEquipe(
    titulo: String,
    valor: String,
    opcoes: List<Pair<String, String>>,
    enabled: Boolean,
    onChange: (String) -> Unit
) {
    var aberto by remember { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(titulo, fontWeight = FontWeight.SemiBold)

        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { aberto = true },
                enabled = enabled,
                modifier = Modifier.fillMaxWidth()
                , shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp), colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = VermelhoOriginal)) {
                Text(
                    opcoes.firstOrNull {
                        it.first == valor
                    }?.second ?: "Selecione"
                )
            }

            DropdownMenu(
                expanded = aberto && enabled,
                onDismissRequest = { aberto = false }
            ) {
                opcoes.forEach { (codigo, nome) ->
                    DropdownMenuItem(
                        text = { Text(nome) },
                        onClick = {
                            aberto = false
                            onChange(codigo)
                        }
                    )
                }
            }
        }
    }
}

private fun funcoesEquipe(): List<Pair<String, String>> {
    return listOf(
        "ATENDIMENTO" to "Atendimento",
        "TRIAGEM" to "Triagem",
        "ESTOQUE" to "Estoque",
        "ADMINISTRADOR" to "Administrador"
    )
}

private fun permissoesEquipe(
    funcao: String
): List<Pair<String, String>> {
    return if (funcao == "ADMINISTRADOR") {
        listOf("ADMINISTRADOR" to "Administrador")
    } else {
        listOf(
            "PADRAO" to "Padrão",
            "SOMENTE_VISUALIZACAO" to "Somente visualização"
        )
    }
}

private fun nomeFuncaoEquipe(valor: String): String {
    return funcoesEquipe().firstOrNull {
        it.first == valor
    }?.second ?: valor
}

private fun nomePermissaoEquipe(valor: String): String {
    return when (valor) {
        "ADMINISTRADOR" -> "Administrador"
        "PADRAO" -> "Padrão"
        "SOMENTE_VISUALIZACAO" -> "Somente visualização"
        else -> valor
    }
}

private fun nomeStatusConviteEquipe(valor: String): String {
    return when (valor) {
        "PENDENTE" -> "Pendente"
        "ACEITO" -> "Aceito"
        "CANCELADO" -> "Cancelado"
        "EXPIRADO" -> "Expirado"
        else -> valor
    }
}

private fun textoEquipe(
    objeto: JSONObject,
    campo: String
): String? {
    return if (objeto.isNull(campo)) {
        null
    } else {
        objeto.optString(campo).takeIf { it.isNotBlank() }
    }
}

private fun opcionalEquipe(valor: String): Any {
    return valor.trim().ifBlank { null } ?: JSONObject.NULL
}

private fun senhaValidaEquipe(senha: String): Boolean {
    return senha.length >= 6 &&
            senha.toByteArray(Charsets.UTF_8).size <= 72 &&
            senha.any { it.isUpperCase() } &&
            senha.any {
                !it.isLetterOrDigit() && !it.isWhitespace()
            }
}