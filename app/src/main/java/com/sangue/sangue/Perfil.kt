package com.sangue.sangue

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.net.toUri
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.json.JSONObject

enum class PerfilRoute {
    MAIN,
    EDITAR,
    NOTIFICACOES,
    PRIVACIDADE,
    AJUDA,
    SOBRE,
    CARTEIRA
}

private val PerfilVermelho = Color(0xFFE21C2C)
private val PerfilTexto = Color(0xFF1E293B)
private val PerfilCinza = Color(0xFF64748B)

// ==========================================
// FLUXO DO PERFIL
// ==========================================

@Composable
fun PerfilScreen(
    onNavigateBack: () -> Unit = {},
    onLogout: () -> Unit = {},
    onNavigateToTriagem: () -> Unit = {}
) {
    var currentRoute by remember {
        mutableStateOf(PerfilRoute.MAIN)
    }

    var carregando by remember {
        mutableStateOf(true)
    }

    var erro by remember {
        mutableStateOf<String?>(null)
    }

    var tentativa by remember {
        mutableStateOf(0)
    }

    var saindo by remember {
        mutableStateOf(false)
    }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(tentativa) {
        carregando = true
        erro = null

        try {
            if (SangueApi.autenticado) {
                SangueApi.atualizarUsuario()
            } else {
                erro = "Entre novamente para acessar seu perfil."
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: ApiException) {
            erro = exception.message
                ?: "Não foi possível atualizar o perfil."
        } catch (exception: Exception) {
            erro = "Não foi possível atualizar o perfil."
        } finally {
            carregando = false
        }
    }

    fun encerrarSessao() {
        if (saindo) return

        saindo = true

        scope.launch {
            try {
                if (SangueApi.autenticado) {
                    SangueApi.sair()
                } else {
                    SangueApi.limparSessao()
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                // sair() já remove a sessão local no finally.
                Toast.makeText(
                    context,
                    "Você saiu deste aparelho. " +
                            "Não foi possível confirmar o encerramento no servidor.",
                    Toast.LENGTH_LONG
                ).show()
            }

            onLogout()
        }
    }

    BackHandler {
        if (!saindo) {
            if (currentRoute == PerfilRoute.MAIN) {
                onNavigateBack()
            } else {
                currentRoute = PerfilRoute.MAIN
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (currentRoute) {
            PerfilRoute.MAIN -> PerfilMainView(
                onBack = onNavigateBack,
                onRoute = { currentRoute = it },
                onLogout = { encerrarSessao() },
                carregando = carregando,
                erro = erro,
                onAtualizar = { tentativa++ }
            )

            PerfilRoute.EDITAR -> PerfilEditarView(
                onBack = {
                    currentRoute = PerfilRoute.MAIN
                }
            )

            PerfilRoute.NOTIFICACOES -> PerfilNotificacoesView(
                onBack = {
                    currentRoute = PerfilRoute.MAIN
                }
            )

            PerfilRoute.PRIVACIDADE -> PerfilPrivacidadeView(
                onBack = {
                    currentRoute = PerfilRoute.MAIN
                },
                onLogout = {
                    encerrarSessao()
                }
            )

            PerfilRoute.AJUDA -> PerfilAjudaView(
                onBack = {
                    currentRoute = PerfilRoute.MAIN
                },
                onTriagem = onNavigateToTriagem
            )

            PerfilRoute.SOBRE -> PerfilSobreView(
                onBack = {
                    currentRoute = PerfilRoute.MAIN
                }
            )

            PerfilRoute.CARTEIRA -> PerfilCarteiraView(
                onBack = {
                    currentRoute = PerfilRoute.MAIN
                }
            )
        }
    }

    if (saindo) {
        Dialog(onDismissRequest = {}) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {
                Row(
                    modifier = Modifier.padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        color = PerfilVermelho
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Text(
                        "Encerrando sessão...",
                        color = PerfilTexto
                    )
                }
            }
        }
    }
}

// ==========================================
// TELA PRINCIPAL
// ==========================================

@Composable
fun PerfilMainView(
    onBack: () -> Unit,
    onRoute: (PerfilRoute) -> Unit,
    onLogout: () -> Unit,
    carregando: Boolean = false,
    erro: String? = null,
    onAtualizar: () -> Unit = {}
) {
    var showLogoutDialog by remember {
        mutableStateOf(false)
    }

    val usuario = SangueApi.usuario

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        HeaderPerfil("Perfil", onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (carregando) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = PerfilVermelho
                )
            }

            erro?.let { mensagem ->
                Text(
                    mensagem,
                    color = PerfilVermelho,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(24.dp)
                )

                TextButton(
                    onClick = onAtualizar,
                    enabled = !carregando
                ) {
                    Text(
                        "Tentar novamente",
                        color = PerfilVermelho
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .size(90.dp)
                    .background(
                        Color(0xFFCBD5E1),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(50.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                usuario.textoPerfil("nome")
                    .ifBlank { "Perfil indisponível" },
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = PerfilTexto,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Text(
                usuario.textoPerfil("email"),
                fontSize = 14.sp,
                color = PerfilCinza,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            if (usuario != null) {
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    tipoSanguineoPerfil(usuario),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = PerfilVermelho
                )

                Text(
                    "Tipo sanguíneo cadastrado",
                    fontSize = 12.sp,
                    color = PerfilCinza
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (usuario != null && !carregando) {
                MenuPerfilItem(
                    Icons.Outlined.Edit,
                    "Editar perfil"
                ) {
                    onRoute(PerfilRoute.EDITAR)
                }

                MenuPerfilItem(
                    Icons.Outlined.Notifications,
                    "Notificações"
                ) {
                    onRoute(PerfilRoute.NOTIFICACOES)
                }

                MenuPerfilItem(
                    Icons.Outlined.Lock,
                    "Privacidade"
                ) {
                    onRoute(PerfilRoute.PRIVACIDADE)
                }

                MenuPerfilItem(
                    Icons.AutoMirrored.Outlined.HelpOutline,
                    "Ajuda"
                ) {
                    onRoute(PerfilRoute.AJUDA)
                }

                MenuPerfilItem(
                    Icons.Outlined.Info,
                    "Sobre o app"
                ) {
                    onRoute(PerfilRoute.SOBRE)
                }

                MenuPerfilItem(
                    Icons.Outlined.CreditCard,
                    "Carteira do Doador"
                ) {
                    onRoute(PerfilRoute.CARTEIRA)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedButton(
                onClick = {
                    showLogoutDialog = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(56.dp),
                border = BorderStroke(
                    1.dp,
                    Color(0xFFE2E8F0)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    "Sair",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = PerfilTexto
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = {
                showLogoutDialog = false
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(24.dp),
            icon = {
                Icon(
                    Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = null,
                    tint = PerfilVermelho
                )
            },
            title = {
                Text("Deseja sair?")
            },
            text = {
                Text(
                    "Você precisará entrar novamente para acessar seus dados."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    }
                ) {
                    Text("Sair", color = PerfilVermelho)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                    }
                ) {
                    Text("Cancelar", color = PerfilCinza)
                }
            }
        )
    }
}

// ==========================================
// EDIÇÃO CONECTADA À API
// ==========================================

@Composable
fun PerfilEditarView(
    onBack: () -> Unit
) {
    val usuario = SangueApi.usuario
    val usuarioId = usuario?.optLong("id")

    var nome by remember(usuarioId) {
        mutableStateOf(usuario.textoPerfil("nome"))
    }

    var email by remember(usuarioId) {
        mutableStateOf(usuario.textoPerfil("email"))
    }

    var telefone by remember(usuarioId) {
        mutableStateOf(usuario.textoPerfil("telefone"))
    }

    var cidade by remember(usuarioId) {
        mutableStateOf(usuario.textoPerfil("cidade"))
    }

    var cpf by remember(usuarioId) {
        mutableStateOf(usuario.textoPerfil("cpf"))
    }

    var senhaAtual by remember {
        mutableStateOf("")
    }

    var salvando by remember {
        mutableStateOf(false)
    }

    var erro by remember {
        mutableStateOf<String?>(null)
    }

    val emailOriginal = usuario.textoPerfil("email")
    val cpfRegistrado = usuario.textoPerfil("cpf").isNotBlank()

    val alterouEmail = !email.trim().equals(
        emailOriginal,
        ignoreCase = true
    )

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val voltar: () -> Unit = {
        if (!salvando) {
            onBack()
        }
    }

    BackHandler {
        voltar()
    }

    fun salvar() {
        if (salvando) return

        if (!SangueApi.autenticado) {
            erro = "Sua sessão expirou. Entre novamente."
            return
        }

        if (nome.trim().length !in 2..100) {
            erro = "Informe um nome entre 2 e 100 caracteres."
            return
        }

        if (
            email.trim().length > 100 ||
            !Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
                .matches(email.trim())
        ) {
            erro = "Informe um e-mail válido."
            return
        }

        if (alterouEmail && senhaAtual.isBlank()) {
            erro = "Informe sua senha atual para alterar o e-mail."
            return
        }

        salvando = true
        erro = null

        scope.launch {
            try {
                SangueApi.salvarPerfil(
                    nome = nome,
                    email = email,
                    telefone = telefone,
                    cidade = cidade,
                    cpf = cpf,
                    senhaAtual = if (alterouEmail) {
                        senhaAtual
                    } else {
                        ""
                    }
                )

                Toast.makeText(
                    context,
                    "Perfil atualizado!",
                    Toast.LENGTH_SHORT
                ).show()

                onBack()
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: ApiException) {
                erro = exception.message
                    ?: "Não foi possível salvar o perfil."
            } catch (exception: Exception) {
                erro = "Não foi possível confirmar a atualização. " +
                        "Volte ao perfil e confira os dados antes de tentar novamente."
            } finally {
                salvando = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        HeaderPerfil("Editar meus dados", voltar)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "Atualize os dados vinculados à sua conta.",
                fontSize = 14.sp,
                color = PerfilCinza
            )

            Spacer(modifier = Modifier.height(24.dp))

            CampoEditarPerfil(
                label = "Nome completo",
                value = nome,
                enabled = !salvando
            ) {
                nome = it
            }

            CampoEditarPerfil(
                label = "E-mail",
                value = email,
                enabled = !salvando
            ) {
                email = it
            }

            CampoEditarPerfil(
                label = "Telefone",
                value = telefone,
                enabled = !salvando
            ) {
                telefone = it
            }

            CampoEditarPerfil(
                label = "Cidade",
                value = cidade,
                enabled = !salvando
            ) {
                cidade = it
            }

            CampoEditarPerfil(
                label = "CPF",
                value = cpf,
                enabled = !salvando,
                readOnly = cpfRegistrado
            ) {
                cpf = it
            }

            if (cpfRegistrado) {
                Text(
                    "O CPF já registrado não pode ser alterado por aqui.",
                    fontSize = 12.sp,
                    color = PerfilCinza,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            CampoEditarPerfil(
                label = "Tipo sanguíneo cadastrado",
                value = tipoSanguineoPerfil(usuario),
                enabled = false,
                readOnly = true,
                onValueChange = {}
            )

            if (alterouEmail) {
                Text(
                    "Para trocar o e-mail, confirme sua senha atual.",
                    fontSize = 13.sp,
                    color = PerfilCinza,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                FormTextField(
                    label = "Senha atual",
                    placeholder = "Informe sua senha",
                    isPassword = true,
                    value = senhaAtual
                ) {
                    if (!salvando) {
                        senhaAtual = it
                    }
                }
            }

            erro?.let {
                Text(
                    it,
                    fontSize = 13.sp,
                    color = PerfilVermelho,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { salvar() },
                enabled = !salvando && usuario != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PerfilVermelho
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    if (salvando) "Salvando..." else "Salvar alterações",
                    fontWeight = FontWeight.Bold
                )
            }

            TextButton(
                onClick = voltar,
                enabled = !salvando,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancelar", color = PerfilCinza)
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ==========================================
// CARTEIRA SEM DADOS FICTÍCIOS
// ==========================================

@Composable
fun PerfilCarteiraView(
    onBack: () -> Unit
) {
    val usuario = SangueApi.usuario
    val context = LocalContext.current

    val identificacao = usuario
        ?.optLong("id")
        ?.takeIf { it > 0 }
        ?.toString()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        HeaderPerfil("Carteira do Doador", onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = PerfilVermelho
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        "Sangue+",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        usuario.textoPerfil("nome")
                            .ifBlank { "Dados indisponíveis" },
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        "Tipo sanguíneo cadastrado",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp
                    )

                    Text(
                        tipoSanguineoPerfil(usuario),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "Identificação da conta",
                fontWeight = FontWeight.Bold,
                color = PerfilCinza
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    identificacao?.let { "#$it" } ?: "Indisponível",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = PerfilTexto,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    enabled = identificacao != null,
                    onClick = {
                        identificacao?.let { id ->
                            val clipboard = context.getSystemService(
                                Context.CLIPBOARD_SERVICE
                            ) as ClipboardManager

                            clipboard.setPrimaryClip(
                                ClipData.newPlainText(
                                    "Identificação da conta Sangue+",
                                    id
                                )
                            )

                            Toast.makeText(
                                context,
                                "Identificação copiada!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                ) {
                    Icon(
                        Icons.Outlined.ContentCopy,
                        contentDescription = "Copiar identificação",
                        tint = PerfilCinza
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            PoliticaItemText(
                "Sobre esta identificação",
                "Este número identifica sua conta no aplicativo. " +
                        "Não é um código oficial emitido pelo hemocentro."
            )

            PoliticaItemText(
                "Tipo sanguíneo",
                "O dado exibido é o cadastrado na conta. " +
                        "Esta tela não comprova confirmação laboratorial."
            )

            PoliticaItemText(
                "No atendimento",
                "Apresente o documento solicitado pelo hemocentro. " +
                        "Esta carteira não substitui documento oficial " +
                        "nem avaliação de aptidão."
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ==========================================
// NOTIFICAÇÕES E PRIVACIDADE
// ==========================================

@Composable
fun PerfilNotificacoesView(
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        HeaderPerfil("Notificações", onBack)

        Column(modifier = Modifier.padding(24.dp)) {
            PoliticaItemText(
                "Recurso em preparação",
                "As preferências e o envio de notificações ainda " +
                        "não estão integrados. Nenhuma configuração " +
                        "de notificação pode ser salva nesta tela."
            )
        }
    }
}

@Composable
fun PerfilPrivacidadeView(
    onBack: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current

    var mostrarExclusao by remember {
        mutableStateOf(false)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        HeaderPerfil("Privacidade", onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            PoliticaItemText(
                "Seus dados",
                "Você pode consultar e editar os dados da conta " +
                        "em Perfil > Editar perfil."
            )

            MenuSubItem(
                "Permissões do aplicativo",
                "Gerenciar nas configurações do Android"
            ) {
                val intent = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    "package:${context.packageName}".toUri()
                )

                try {
                    context.startActivity(intent)
                } catch (_: Exception) {
                    Toast.makeText(
                        context,
                        "Abra as configurações do Android e procure Sangue+.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            PoliticaItemText(
                "Preferências de compartilhamento",
                "O gerenciamento dessas preferências ainda não " +
                        "está disponível no aplicativo."
            )

            MenuSubItem(
                "Excluir conta",
                "Consultar disponibilidade"
            ) {
                mostrarExclusao = true
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (mostrarExclusao) {
        AlertDialog(
            onDismissRequest = {
                mostrarExclusao = false
            },
            title = {
                Text("Exclusão de conta")
            },
            text = {
                Text(
                    "A exclusão de conta ainda não foi implementada. " +
                            "Nenhum dado será removido ao fechar esta mensagem."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        mostrarExclusao = false
                    }
                ) {
                    Text("Entendi", color = PerfilVermelho)
                }
            }
        )
    }
}

// ==========================================
// AJUDA E SOBRE
// ==========================================

@Composable
fun PerfilAjudaView(
    onBack: () -> Unit,
    onTriagem: () -> Unit
) {
    var busca by remember {
        mutableStateOf("")
    }

    val perguntas = listOf(
        "Como atualizar meus dados?" to
                "Acesse Perfil > Editar perfil. Para trocar o e-mail, " +
                "informe também sua senha atual.",

        "Por que o intervalo pode estar pendente?" to
                "O cálculo depende das doações registradas e da regra " +
                "de acompanhamento definida pelo hemocentro.",

        "O que significa o tipo sanguíneo exibido?" to
                "É o tipo cadastrado na conta. A exibição no aplicativo " +
                "não representa comprovação laboratorial.",

        "Como sair da minha conta?" to
                "Na tela principal do Perfil, toque em Sair e confirme."
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        HeaderPerfil("Central de ajuda", onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            OutlinedTextField(
                value = busca,
                onValueChange = { busca = it },
                label = { Text("Buscar dúvida") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            val resultados = perguntas.filter {
                it.first.contains(busca, ignoreCase = true) ||
                        it.second.contains(busca, ignoreCase = true)
            }

            resultados.forEach { (titulo, resposta) ->
                FaqCard(
                    title = titulo,
                    text = resposta,
                    btnText = "",
                    onBtnClick = {}
                )
            }

            if (resultados.isEmpty()) {
                Text(
                    "Nenhuma dúvida encontrada.",
                    color = PerfilCinza,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onTriagem,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Abrir triagem",
                    color = PerfilVermelho
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun FaqCard(
    title: String,
    text: String,
    btnText: String,
    onBtnClick: () -> Unit
) {
    var expanded by remember {
        mutableStateOf(false)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = PerfilTexto,
                modifier = Modifier.weight(1f)
            )

            Icon(
                if (expanded) {
                    Icons.AutoMirrored.Filled.ArrowBack
                } else {
                    Icons.AutoMirrored.Filled.ArrowForwardIos
                },
                contentDescription = null,
                tint = PerfilCinza,
                modifier = Modifier.size(16.dp)
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.padding(bottom = 16.dp)) {
                Text(
                    text,
                    fontSize = 13.sp,
                    color = PerfilCinza,
                    lineHeight = 20.sp
                )

                if (btnText.isNotBlank()) {
                    TextButton(onClick = onBtnClick) {
                        Text(
                            btnText,
                            color = PerfilVermelho
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = Color(0xFFF1F5F9))
    }
}

@Composable
fun PerfilSobreView(
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        HeaderPerfil("Sobre o app", onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Icon(
                Icons.Outlined.WaterDrop,
                contentDescription = null,
                tint = PerfilVermelho,
                modifier = Modifier.size(48.dp)
            )

            Text(
                "Sangue+",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = PerfilVermelho
            )

            Spacer(modifier = Modifier.height(24.dp))

            PoliticaItemText(
                "Nossa missão",
                "Conectar doadores e hemocentros, apoiando " +
                        "a organização e o acompanhamento das doações."
            )

            PoliticaItemText(
                "Projeto acadêmico",
                "Aplicativo em desenvolvimento. Os recursos são " +
                        "integrados e validados por etapas."
            )

            PoliticaItemText(
                "Documentação",
                "Os documentos definitivos de termos de uso, " +
                        "privacidade e créditos ainda precisam ser " +
                        "consolidados para publicação."
            )
        }
    }
}

// ==========================================
// COMPONENTES E FORMATAÇÃO
// ==========================================

private fun JSONObject?.textoPerfil(
    chave: String
): String {
    val objeto = this ?: return ""

    return if (objeto.isNull(chave)) {
        ""
    } else {
        objeto.optString(chave).trim()
    }
}

private fun tipoSanguineoPerfil(
    usuario: JSONObject?
): String {
    val tipo = usuario.textoPerfil("tipoSanguineo")

    return if (
        tipo in setOf(
            "A+", "A-", "B+", "B-",
            "AB+", "AB-", "O+", "O-"
        )
    ) {
        tipo
    } else {
        "Não informado"
    }
}

@Composable
private fun CampoEditarPerfil(
    label: String,
    value: String,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        enabled = enabled,
        readOnly = readOnly,
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = PerfilVermelho,
            focusedLabelColor = PerfilVermelho,
            unfocusedBorderColor = Color(0xFFE2E8F0),
            disabledTextColor = PerfilTexto
        )
    )
}

@Composable
fun HeaderPerfil(
    title: String,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = 24.dp,
                bottom = 16.dp,
                start = 16.dp,
                end = 16.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Voltar",
                tint = PerfilTexto
            )
        }

        Text(
            title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = PerfilTexto,
            modifier = Modifier
                .weight(1f)
                .offset(x = (-24).dp),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun MenuPerfilItem(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(
                    vertical = 16.dp,
                    horizontal = 24.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = PerfilCinza,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = PerfilTexto,
                modifier = Modifier.weight(1f)
            )

            Icon(
                Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(16.dp)
            )
        }

        HorizontalDivider(
            color = Color(0xFFF1F5F9),
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}

@Composable
fun MenuSubItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = PerfilTexto
                )

                if (subtitle.isNotEmpty()) {
                    Text(
                        subtitle,
                        fontSize = 12.sp,
                        color = PerfilCinza
                    )
                }
            }

            Icon(
                Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(16.dp)
            )
        }

        HorizontalDivider(color = Color(0xFFF1F5F9))
    }
}

@Composable
fun ToggleItem(
    title: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = PerfilTexto,
            modifier = Modifier.weight(1f)
        )

        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = PerfilVermelho
            )
        )
    }

    HorizontalDivider(color = Color(0xFFF1F5F9))
}

@Composable
fun ToggleItemSubtitle(
    title: String,
    subtitle: String,
    isChecked: Boolean,
    disabled: Boolean = false,
    statusText: String = "",
    onCheckedChange: (Boolean) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                title,
                fontWeight = FontWeight.Bold,
                color = PerfilTexto,
                modifier = Modifier.weight(1f)
            )

            if (disabled) {
                Text(
                    statusText,
                    color = PerfilCinza,
                    fontSize = 12.sp
                )
            } else {
                Switch(
                    checked = isChecked,
                    onCheckedChange = onCheckedChange,
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = PerfilVermelho
                    )
                )
            }
        }

        Text(
            subtitle,
            fontSize = 12.sp,
            color = PerfilCinza
        )
    }

    HorizontalDivider(color = Color(0xFFF1F5F9))
}

@Composable
fun PoliticaItemText(
    title: String,
    text: String
) {
    Column(modifier = Modifier.padding(bottom = 16.dp)) {
        Text(
            title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = PerfilTexto
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text,
            fontSize = 13.sp,
            color = PerfilCinza,
            lineHeight = 20.sp
        )
    }
}