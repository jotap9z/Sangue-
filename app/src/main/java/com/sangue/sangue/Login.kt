package com.sangue.sangue

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ==========================================
// MÁQUINA DE ESTADOS DO FLUXO DE AUTENTICAÇÃO
// ==========================================
enum class AuthStep {
    LOGIN,
    // Doador
    CADASTRO_DADOS, CADASTRO_SANGUE, CADASTRO_SUCESSO,
    // Hemocentro
    HEMO_DADOS, HEMO_RESPONSAVEL, HEMO_VERIFICACAO, HEMO_SUCESSO
}

@Composable
fun AuthScreen(
    onLoginSuccess: (Boolean) -> Unit = {}, // Redireciona dependendo do tipo de login
) {
    var currentStep by remember { mutableStateOf(AuthStep.LOGIN) }

    // Intercepta o botão de voltar do celular
    BackHandler {
        when (currentStep) {
            AuthStep.CADASTRO_SANGUE -> currentStep = AuthStep.CADASTRO_DADOS
            AuthStep.CADASTRO_DADOS -> currentStep = AuthStep.LOGIN
            AuthStep.HEMO_DADOS -> currentStep = AuthStep.LOGIN
            AuthStep.HEMO_RESPONSAVEL -> currentStep = AuthStep.HEMO_DADOS
            AuthStep.HEMO_VERIFICACAO -> currentStep = AuthStep.HEMO_RESPONSAVEL
            AuthStep.CADASTRO_SUCESSO, AuthStep.HEMO_SUCESSO -> {} // Trava, obriga a clicar no botão final
            AuthStep.LOGIN -> {} // Sai do app (comportamento padrão)
        }
    }

    when (currentStep) {
        AuthStep.LOGIN -> LoginView(
            onEntrar = { onLoginSuccess(false) },
            onCriarConta = { currentStep = AuthStep.CADASTRO_DADOS },
        ) {
            //  MUDANÇA AQUI: Passamos 'true' indicando que o login foi de Hemocentro
            onLoginSuccess(true)
        }

        // Telas do Doador
        AuthStep.CADASTRO_DADOS -> CadastroDadosView(onBack = { currentStep = AuthStep.LOGIN }) {
            currentStep = AuthStep.CADASTRO_SANGUE
        }
        AuthStep.CADASTRO_SANGUE -> CadastroSangueView(onBack = { currentStep = AuthStep.CADASTRO_DADOS }) {
            currentStep = AuthStep.CADASTRO_SUCESSO
        }
        AuthStep.CADASTRO_SUCESSO -> CadastroSucessoView { onLoginSuccess(false) }

        // Telas do Hemocentro (Ficarão inativas/escondidas por enquanto, já que o botão vai direto pro login)
        AuthStep.HEMO_DADOS -> CadastroHemoDadosView(onBack = { currentStep = AuthStep.LOGIN }) { currentStep = AuthStep.HEMO_RESPONSAVEL }
        AuthStep.HEMO_RESPONSAVEL -> CadastroHemoResponsavelView(onBack = { currentStep = AuthStep.HEMO_DADOS }) { currentStep = AuthStep.HEMO_VERIFICACAO }
        AuthStep.HEMO_VERIFICACAO -> CadastroHemoVerificacaoView(onBack = { currentStep = AuthStep.HEMO_RESPONSAVEL }) { currentStep = AuthStep.HEMO_SUCESSO }
        AuthStep.HEMO_SUCESSO -> CadastroHemoSucessoView { currentStep = AuthStep.LOGIN }
    }
}

// ==========================================
// 1. TELA DE LOGIN
// ==========================================
@Composable
fun LoginView(onEntrar: () -> Unit, onCriarConta: () -> Unit, onCadastroHemocentro: () -> Unit) {
    val sangueRed = Color(0xFFE21C2C)
    val textDark = Color(0xFF1E293B)
    val textGray = Color(0xFF64748B)

    var email by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }

    // 🔥 Estados para o Card/Modal de Recuperação de Senha
    var showRecuperarSenhaModal by remember { mutableStateOf(false) }
    var showSucessoModal by remember { mutableStateOf(false) }
    var emailRecuperacao by remember { mutableStateOf("") }

    // Usando Box para sobrepor o Modal
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().background(Color.White).padding(24.dp).verticalScroll(rememberScrollState())) {
            Spacer(modifier = Modifier.height(40.dp))

            Text("Sangue+", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = sangueRed)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Entrar na sua conta", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = textDark)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Acesse seus agendamentos, histórico e carteira do doador.", fontSize = 14.sp, color = textGray)

            Spacer(modifier = Modifier.height(32.dp))

            FormTextField("E-mail", "ana@email.com", isRequired = true, value = email) { email = it }
            FormTextField("Senha", "••••••••", isRequired = true, isPassword = true, value = senha) { senha = it }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Esqueci minha senha",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = sangueRed,
                modifier = Modifier
                    .align(Alignment.End)
                    .clickable { showRecuperarSenhaModal = true } // 🔥 Aciona o Card de recuperação
                    .padding(vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onEntrar,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = sangueRed),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Entrar", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE2E8F0))
                Text(" ou ", color = textGray, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 8.dp))
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE2E8F0))
            }
            Spacer(modifier = Modifier.height(24.dp))

            OutlinedButton(onClick = onCriarConta, modifier = Modifier.fillMaxWidth().height(56.dp), border = BorderStroke(1.dp, Color(0xFFFECACA)), shape = RoundedCornerShape(16.dp)) {
                Text("Criar conta", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = sangueRed)
            }

            Spacer(modifier = Modifier.height(32.dp))

            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)), border = BorderStroke(1.dp, Color(0xFFFECACA)), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Você representa um hemocentro?", fontWeight = FontWeight.Bold, color = textDark)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Acesse a área institucional para gerenciar estoque, agendamentos e chamados.", fontSize = 12.sp, color = textGray)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = onCadastroHemocentro,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        border = BorderStroke(1.dp, Color(0xFFFECACA)),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text("Acessar área do hemocentro", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = sangueRed)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Ao continuar, você concorda com nossos Termos e Privacidade.", fontSize = 11.sp, color = textGray, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(24.dp))
        }

        // 🔥 MODAL: INFORMAR E-MAIL PARA RECUPERAÇÃO
        if (showRecuperarSenhaModal) {
            AlertDialog(
                onDismissRequest = { showRecuperarSenhaModal = false },
                containerColor = Color.White,
                shape = RoundedCornerShape(24.dp),
                title = {
                    Text("Recuperar senha", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = textDark, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("Digite o e-mail associado à sua conta e enviaremos um link para redefinir sua senha.", fontSize = 14.sp, color = textGray, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(16.dp))
                        FormTextField("Seu e-mail", "ex: ana@email.com", isRequired = false, value = emailRecuperacao) { emailRecuperacao = it }
                    }
                },
                confirmButton = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = {
                                if (emailRecuperacao.isNotBlank()) {
                                    showRecuperarSenhaModal = false
                                    showSucessoModal = true
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = sangueRed),
                            shape = RoundedCornerShape(12.dp),
                            enabled = emailRecuperacao.isNotBlank()
                        ) {
                            Text("Enviar link", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { showRecuperarSenhaModal = false },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Cancelar", color = textDark, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            )
        }

        // 🔥 MODAL: SUCESSO NO ENVIO DO E-MAIL
        if (showSucessoModal) {
            AlertDialog(
                onDismissRequest = { showSucessoModal = false },
                containerColor = Color.White,
                shape = RoundedCornerShape(24.dp),
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier.size(64.dp).background(Color(0xFFDCFCE7), CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(32.dp))
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("E-mail enviado!", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = textDark, textAlign = TextAlign.Center)
                    }
                },
                text = {
                    Text("As instruções de recuperação foram enviadas para:\n$emailRecuperacao\n\nVerifique sua caixa de entrada.", fontSize = 14.sp, color = textGray, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                },
                confirmButton = {
                    Button(
                        onClick = { showSucessoModal = false; emailRecuperacao = "" },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = sangueRed),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Voltar para o login", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            )
        }
    }
}

// ==========================================
// TELAS DO HEMOCENTRO
// ==========================================

@Composable
fun CadastroHemoDadosView(onBack: () -> Unit, onContinuar: () -> Unit) {
    val sangueRed = Color(0xFFE21C2C)
    val textDark = Color(0xFF1E293B)
    val textGray = Color(0xFF64748B)

    var nome by remember { mutableStateOf("") }; var cnpj by remember { mutableStateOf("") }; var cnes by remember { mutableStateOf("") }
    var telefone by remember { mutableStateOf("") }; var email by remember { mutableStateOf("") }; var endereco by remember { mutableStateOf("") }
    var cidade by remember { mutableStateOf("") }; var cep by remember { mutableStateOf("") }; var observacao by remember { mutableStateOf("") }

    val isFormValid = nome.isNotBlank() && cnpj.isNotBlank() && cnes.isNotBlank() &&
            telefone.isNotBlank() && email.isNotBlank() && endereco.isNotBlank() &&
            cidade.isNotBlank() && cep.isNotBlank()

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        HeaderApp("Cadastrar hemocentro", onBack)

        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp).verticalScroll(rememberScrollState())) {
            HemoStepper(currentStep = 1)

            Text("Dados da instituição", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = textDark)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Informe os dados oficiais do hemocentro.", fontSize = 14.sp, color = textGray)
            Spacer(modifier = Modifier.height(24.dp))

            FormTextField("Nome do hemocentro", "Hemocentro São Paulo", isRequired = true, value = nome) { nome = it }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                FormTextField("CNPJ", "00.000.000/0001-00", Modifier.weight(1f), isRequired = true, value = cnpj) { cnpj = it }
                FormTextField("CNES", "1234567", Modifier.weight(1f), isRequired = true, value = cnes) { cnes = it }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                FormTextField("Telefone institucional", "(11) 3333-2222", Modifier.weight(1f), isRequired = true, value = telefone) { telefone = it }
                FormTextField("E-mail institucional", "contato@hemo.org", Modifier.weight(1f), isRequired = true, value = email) { email = it }
            }
            FormTextField("Endereço", "Av. Paulista, 2073", isRequired = true, value = endereco) { endereco = it }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                FormTextField("Cidade / Estado", "São Paulo - SP", Modifier.weight(1f), isRequired = true, value = cidade) { cidade = it }
                FormTextField("CEP", "01311-200", Modifier.weight(1f), isRequired = true, value = cep) { cep = it }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text("Observação", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textDark)
            Text("Use dados reais da instituição para agilizar a aprovação.", fontSize = 12.sp, color = textGray)

            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onContinuar,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = sangueRed),
                shape = RoundedCornerShape(16.dp),
                enabled = isFormValid
            ) {
                Text("Continuar", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun CadastroHemoResponsavelView(onBack: () -> Unit, onContinuar: () -> Unit) {
    val sangueRed = Color(0xFFE21C2C)
    val textDark = Color(0xFF1E293B)

    var nome by remember { mutableStateOf("") }; var cpf by remember { mutableStateOf("") }; var cargo by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }; var telefone by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }; var confirmarSenha by remember { mutableStateOf("") }
    var isChecked by remember { mutableStateOf(false) }

    // Regras de validação em tempo real
    val hasMinLength = senha.length >= 6
    val hasUpperCase = senha.any { it.isUpperCase() }
    val hasSpecialChar = senha.any { !it.isLetterOrDigit() }
    val isFormValid = nome.isNotBlank() && cpf.isNotBlank() && cargo.isNotBlank() &&
            email.isNotBlank() && telefone.isNotBlank() &&
            senha.isNotBlank() && senha == confirmarSenha &&
            hasMinLength && hasUpperCase && hasSpecialChar && isChecked

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        HeaderApp("Cadastrar hemocentro", onBack)

        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp).verticalScroll(rememberScrollState())) {
            HemoStepper(currentStep = 2)

            Text("Responsável pela conta", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = textDark)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Informe quem vai administrar o acesso institucional.", fontSize = 14.sp, color = Color(0xFF64748B))
            Spacer(modifier = Modifier.height(24.dp))

            FormTextField("Nome do responsável", "Marina Oliveira", isRequired = true, value = nome) { nome = it }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                FormTextField("CPF", "000.000.000-00", Modifier.weight(1f), isRequired = true, value = cpf) { cpf = it }
                FormTextField("Cargo / função", "Coordenadora", Modifier.weight(1f), isRequired = true, value = cargo) { cargo = it }
            }
            FormTextField("E-mail", "marina@hemo.org", isRequired = true, value = email) { email = it }
            FormTextField("Telefone", "(11) 98888-7777", isRequired = true, value = telefone) { telefone = it }
            FormTextField("Senha", "••••••••", isPassword = true, isRequired = true, value = senha) { senha = it }
            FormTextField("Confirmar senha", "••••••••", isPassword = true, isRequired = true, value = confirmarSenha) { confirmarSenha = it }

            // Feedbacks visuais das regras da senha
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                RegraSenha("Mínimo de 6 caracteres", hasMinLength)
                Spacer(modifier = Modifier.height(4.dp))
                RegraSenha("Pelo menos 1 letra maiúscula", hasUpperCase)
                Spacer(modifier = Modifier.height(4.dp))
                RegraSenha("Pelo menos 1 caractere especial (!@#\$%&*)", hasSpecialChar)
            }

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 16.dp).clickable { isChecked = !isChecked }) {
                Checkbox(checked = isChecked, onCheckedChange = { isChecked = it }, colors = CheckboxDefaults.colors(checkedColor = sangueRed))
                Text("Confirmo que estou autorizado(a) a representar a instituição", fontSize = 12.sp, color = textDark)
            }

            Button(
                onClick = onContinuar,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = sangueRed),
                shape = RoundedCornerShape(16.dp),
                enabled = isFormValid
            ) {
                Text("Continuar", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun CadastroHemoVerificacaoView(onBack: () -> Unit, onEnviar: () -> Unit) {
    val sangueRed = Color(0xFFE21C2C)
    val textDark = Color(0xFF1E293B)
    val textGray = Color(0xFF64748B)

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        HeaderApp("Cadastrar hemocentro", onBack)

        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp).verticalScroll(rememberScrollState())) {
            HemoStepper(currentStep = 3)

            Text("Verificação do hemocentro", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = textDark)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Revise os dados. O cadastro passará por análise antes da ativação.", fontSize = 14.sp, color = textGray)
            Spacer(modifier = Modifier.height(24.dp))

            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Hemocentro São Paulo", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textDark)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("CNPJ: 00.000.000/0001-00", fontSize = 13.sp, color = textGray)
                    Text("CNES: 1234567", fontSize = 13.sp, color = textGray)
                    Text("Responsável: Marina Oliveira", fontSize = 13.sp, color = textGray)
                    Text("E-mail: contato@hemo.org", fontSize = 13.sp, color = Color(0xFF3B82F6))
                    Text("Endereço: Av. Paulista, 2073 - SP", fontSize = 13.sp, color = textGray)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFE2E8F0)), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Como funciona a análise", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textDark)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("• Verificamos os dados institucionais informados", fontSize = 13.sp, color = textGray)
                    Text("• Confirmamos se o responsável está vinculado", fontSize = 13.sp, color = textGray)
                    Text("• Liberamos o acesso institucional após aprovação", fontSize = 13.sp, color = textGray)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)), border = BorderStroke(1.dp, Color(0xFFFDE68A)), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Prazo estimado", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB45309))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("A análise pode levar até 2 dias úteis.", fontSize = 13.sp, color = Color(0xFF92400E))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = onEnviar, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = sangueRed), shape = RoundedCornerShape(16.dp)) {
                Text("Enviar cadastro", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun CadastroHemoSucessoView(onVoltarLogin: () -> Unit) {
    val sangueRed = Color(0xFFE21C2C)
    val textDark = Color(0xFF1E293B)

    Column(modifier = Modifier.fillMaxSize().background(Color.White).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(modifier = Modifier.height(40.dp))
        Text("Sangue+", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = sangueRed, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)

        Spacer(modifier = Modifier.weight(1f))

        Box(modifier = Modifier.size(100.dp).background(Color(0xFF22C55E), CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(50.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Cadastro enviado", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = textDark)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Recebemos os dados do hemocentro.\nAgora sua solicitação está em análise.", fontSize = 14.sp, color = Color(0xFF64748B), textAlign = TextAlign.Center)

        Spacer(modifier = Modifier.height(40.dp))

        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)), border = BorderStroke(1.dp, Color(0xFFFECACA)), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Próximos passos", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = textDark)
                Spacer(modifier = Modifier.height(12.dp))
                Text("• Você receberá um e-mail com a aprovação", fontSize = 13.sp, color = Color(0xFF64748B))
                Text("• Se necessário, poderemos pedir confirmação", fontSize = 13.sp, color = Color(0xFF64748B))
                Text("• Após aprovado, o acesso institucional será liberado", fontSize = 13.sp, color = Color(0xFF64748B))
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(onClick = onVoltarLogin, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = sangueRed), shape = RoundedCornerShape(16.dp)) {
            Text("Voltar para o login", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

// ==========================================
// TELAS DO DOADOR
// ==========================================

@Composable
fun CadastroDadosView(onBack: () -> Unit, onContinuar: () -> Unit) {
    val sangueRed = Color(0xFFE21C2C)

    var nome by remember { mutableStateOf("") }; var cpf by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }; var telefone by remember { mutableStateOf("") }
    var cidade by remember { mutableStateOf("") }; var senha by remember { mutableStateOf("") }
    var confirmarSenha by remember { mutableStateOf("") }
    var isChecked by remember { mutableStateOf(false) }

    // Regras de validação
    val hasMinLength = senha.length >= 6
    val hasUpperCase = senha.any { it.isUpperCase() }
    val hasSpecialChar = senha.any { !it.isLetterOrDigit() }

    val isFormValid = nome.isNotBlank() && cpf.isNotBlank() && email.isNotBlank() &&
            telefone.isNotBlank() && cidade.isNotBlank() &&
            senha.isNotBlank() && senha == confirmarSenha &&
            hasMinLength && hasUpperCase && hasSpecialChar && isChecked

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        HeaderApp("Criar conta", onBack)

        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp).verticalScroll(rememberScrollState())) {
            Text("Seus dados básicos", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
            Spacer(modifier = Modifier.height(4.dp))
            Text("Preencha as informações para criar sua conta no Sangue+.", fontSize = 14.sp, color = Color(0xFF64748B))
            Spacer(modifier = Modifier.height(24.dp))

            FormTextField("Nome completo", "Ana Silva", isRequired = true, value = nome) { nome = it }
            FormTextField("CPF", "000.000.000-00", isRequired = true, value = cpf) { cpf = it }
            FormTextField("E-mail", "ana@email.com", isRequired = true, value = email) { email = it }
            FormTextField("Telefone", "(11) 99999-9999", isRequired = true, value = telefone) { telefone = it }
            FormTextField("Cidade", "São Paulo - SP", isRequired = true, value = cidade) { cidade = it }
            FormTextField("Senha", "••••••••", isRequired = true, isPassword = true, value = senha) { senha = it }
            FormTextField("Confirmar senha", "••••••••", isRequired = true, isPassword = true, value = confirmarSenha) { confirmarSenha = it }

            // Exibição das regras da senha
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                RegraSenha("Mínimo de 6 caracteres", hasMinLength)
                Spacer(modifier = Modifier.height(4.dp))
                RegraSenha("Pelo menos 1 letra maiúscula", hasUpperCase)
                Spacer(modifier = Modifier.height(4.dp))
                RegraSenha("Pelo menos 1 caractere especial (!@#\$%&*)", hasSpecialChar)
            }

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 16.dp).clickable { isChecked = !isChecked }) {
                Checkbox(checked = isChecked, onCheckedChange = { isChecked = it }, colors = CheckboxDefaults.colors(checkedColor = sangueRed))
                Text("Concordo com os Termos e a Política de Privacidade", fontSize = 12.sp, color = Color(0xFF1E293B))
            }

            Button(
                onClick = onContinuar,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = sangueRed),
                shape = RoundedCornerShape(16.dp),
                enabled = isFormValid
            ) {
                Text("Continuar", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun CadastroSangueView(onBack: () -> Unit, onFinalizar: () -> Unit) {
    val sangueRed = Color(0xFFE21C2C)
    val textDark = Color(0xFF1E293B)
    var selectedBlood by remember { mutableStateOf("O+") }
    val bloodTypes = listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        HeaderApp("Tipo sanguíneo", onBack)

        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp).verticalScroll(rememberScrollState())) {
            Text("Você sabe seu tipo sanguíneo?", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = textDark)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Selecione uma opção abaixo. Se você não souber, escolha \"Não sei\". O hemocentro poderá verificar essa informação após sua doação.", fontSize = 14.sp, color = Color(0xFF64748B), lineHeight = 20.sp)
            Spacer(modifier = Modifier.height(32.dp))

            bloodTypes.chunked(4).forEach { rowItems ->
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    rowItems.forEach { type ->
                        val isSelected = selectedBlood == type
                        Card(modifier = Modifier.weight(1f).aspectRatio(1f).clickable { selectedBlood = type }, colors = CardDefaults.cardColors(containerColor = if (isSelected) sangueRed else Color.White), border = if (!isSelected) BorderStroke(1.dp, Color(0xFFE2E8F0)) else null, shape = RoundedCornerShape(12.dp)) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(type, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = if (isSelected) Color.White else textDark) }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(onClick = { selectedBlood = "Não sei" }, modifier = Modifier.fillMaxWidth().height(48.dp), border = BorderStroke(1.dp, Color(0xFFE2E8F0)), shape = RoundedCornerShape(12.dp)) {
                Text("Não sei meu tipo sanguíneo", color = textDark, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(32.dp))
            Text("Status da informação", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textDark)
            Spacer(modifier = Modifier.height(12.dp))
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)), border = BorderStroke(1.dp, Color(0xFFFECACA)), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Close, contentDescription = null, tint = sangueRed, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Informado por você", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = sangueRed)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Ainda não verificado", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textDark)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Depois da sua primeira doação, o hemocentro poderá confirmar seu tipo sanguíneo e marcar esta informação como verificada.", fontSize = 12.sp, color = Color(0xFF64748B), lineHeight = 18.sp)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = onFinalizar, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = sangueRed), shape = RoundedCornerShape(12.dp)) {
                Text("Salvar e continuar", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = onFinalizar, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text("Pular por enquanto", color = sangueRed, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun CadastroSucessoView(onComecar: () -> Unit) {
    val sangueRed = Color(0xFFE21C2C)
    Column(modifier = Modifier.fillMaxSize().background(Color.White).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(modifier = Modifier.height(40.dp))
        Text("Sangue+", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = sangueRed, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
        Spacer(modifier = Modifier.weight(1f))
        Box(modifier = Modifier.size(100.dp).background(Color(0xFF22C55E), CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(50.dp))
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text("Conta criada!", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E293B))
        Spacer(modifier = Modifier.height(8.dp))
        Text("Seu cadastro foi concluído com sucesso.\nAgora você já pode acessar o app.", fontSize = 14.sp, color = Color(0xFF64748B), textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(40.dp))

        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)), border = BorderStroke(1.dp, Color(0xFFE2E8F0)), shape = RoundedCornerShape(12.dp)) {
            Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                Text("Próximo passo", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                Spacer(modifier = Modifier.height(8.dp))
                Text("Você poderá completar a triagem quando quiser doar e acompanhar seus agendamentos, histórico e conquistas dentro do Sangue+.", fontSize = 12.sp, color = Color(0xFF64748B), lineHeight = 18.sp)
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        Button(onClick = onComecar, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = sangueRed), shape = RoundedCornerShape(16.dp)) {
            Text("Começar", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

// ==========================================
// COMPONENTES REUTILIZÁVEIS GERAIS
// ==========================================

@Composable
fun HeaderApp(title: String, onBack: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 16.dp, start = 16.dp, end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", tint = Color(0xFF1E293B)) }
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B), modifier = Modifier.weight(1f).offset(x = (-24).dp), textAlign = TextAlign.Center)
    }
}

@Composable
fun FormTextField(
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    isRequired: Boolean = false,
    value: String = "",
    onValueChange: (String) -> Unit = {}
) {
    var passwordVisible by remember { mutableStateOf(false) }

    Column(modifier = modifier.padding(bottom = 16.dp)) {
        Row {
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
            if (isRequired) {
                Text(" *", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE21C2C))
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = Color(0xFF94A3B8)) },
            visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
            trailingIcon = if (isPassword) {
                {
                    Text(
                        text = if (passwordVisible) "Ocultar" else "Mostrar",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B),
                        modifier = Modifier.padding(end = 16.dp).clickable { passwordVisible = !passwordVisible }
                    )
                }
            } else null,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color(0xFFE2E8F0),
                focusedBorderColor = Color(0xFFE21C2C),
            ),
        )
    }
}

@Composable
fun RegraSenha(texto: String, cumprida: Boolean) {
    val cor = if (cumprida) Color(0xFF16A34A) else Color(0xFF64748B)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = if (cumprida) Icons.Default.Check else Icons.Outlined.Close,
            contentDescription = null,
            tint = cor,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = texto,
            fontSize = 12.sp,
            color = cor,
            textDecoration = if (cumprida) TextDecoration.LineThrough else TextDecoration.None
        )
    }
}

@Composable
fun HemoStepper(currentStep: Int) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        HemoStepCircle(step = 1, currentStep = currentStep)
        Box(modifier = Modifier.width(40.dp).height(2.dp).background(if (currentStep >= 2) Color(0xFFE21C2C) else Color(0xFFE2E8F0)))
        HemoStepCircle(step = 2, currentStep = currentStep)
        Box(modifier = Modifier.width(40.dp).height(2.dp).background(if (currentStep == 3) Color(0xFFE21C2C) else Color(0xFFE2E8F0)))
        HemoStepCircle(step = 3, currentStep = currentStep)
    }
}

@Composable
fun HemoStepCircle(step: Int, currentStep: Int) {
    val isActiveOrDone = step <= currentStep
    Box(modifier = Modifier.size(36.dp).background(if (isActiveOrDone) Color(0xFFE21C2C) else Color(0xFFF1F5F9), CircleShape), contentAlignment = Alignment.Center) {
        Text(step.toString(), color = if (isActiveOrDone) Color.White else Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
    }
}