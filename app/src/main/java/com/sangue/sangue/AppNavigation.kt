package com.sangue.sangue

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun AppNavigation() {
    // Controlador central que guarda o histórico de telas
    val navController = rememberNavController()

    // O aplicativo inicia pela Landing Page
    NavHost(navController = navController, startDestination = "landing_page") {

        // ==========================================
        // 0. AUTENTICAÇÃO E CADASTRO
        // ==========================================
        composable("auth") {
            AuthScreen(
                onLoginSuccess = { isHemocentro ->
                    if (isHemocentro) {
                        navController.navigate("hemo_home") {
                            popUpTo("auth") { inclusive = true }
                        }
                    } else {
                        navController.navigate("home") {
                            popUpTo("auth") { inclusive = true }
                        }
                    }
                }
            )
        }

        // ==========================================
        // 1. LANDING PAGE (Início do Aplicativo)
        // ==========================================
        composable("landing_page") {
            LandPageScreen(
                onNavigateToLogin = { navController.navigate("auth") },
                onNavigateToCheckin = { navController.navigate("checkin") },
                onNavigateToEmergencia = { navController.navigate("emergencia") },
                onNavigateToHistorico = { navController.navigate("historico") },
                onNavigateToPerfil = { navController.navigate("perfil") }
            )
        }

        // ==========================================
        // 2. HOME DASHBOARD (Doador)
        // ==========================================
        composable("home") {
            HomeScreen(
                onNavigateToInicio = { /* Já está na home */ },
                onNavigateToVerificacao = { navController.navigate("posso_doar") },
                onNavigateToCheckin = { navController.navigate("checkin") },
                onNavigateToEmergencia = { navController.navigate("emergencia") },
                onNavigateToHistorico = { navController.navigate("historico") },
                onNavigateToPerfil = { navController.navigate("perfil") },
                onNavigateToEstoque = { navController.navigate("estoque") },
                onNavigateToHemocentros = { navController.navigate("hemocentros") },
            )
        }

        // ==========================================
        // 3. CHECK-IN
        // ==========================================
        composable("checkin") {
            CheckInScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateHome = {
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                },
            )
        }

        // ==========================================
        // 4. PERFIL
        // ==========================================
        composable("perfil") {
            PerfilScreen(
                onNavigateBack = { navController.popBackStack() },
                onLogout = {
                    navController.navigate("landing_page") {
                        popUpTo(0) // Limpa tudo e volta para a tela inicial
                    }
                },
                onNavigateToTriagem = { navController.navigate("posso_doar") }
            )
        }

        // ==========================================
        // 5. CONQUISTAS
        // ==========================================
        composable("conquistas") {
            ConquistasScreen { navController.popBackStack() }
        }

        // ==========================================
        // 6. POSSO DOAR? (Triagem)
        // ==========================================
        composable("posso_doar") {
            PossoDoarScreen(
                onNavigateToResultado = { navController.navigate("resultado") },
            ) { navController.popBackStack() }
        }

        // ==========================================
        // 7. RESULTADO (Apto)
        // ==========================================
        composable("resultado") {
            ResultadoScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAgendamento = { navController.navigate("agendamento") },
            ) { navController.navigate("hemocentros") }
        }

        // ==========================================
        // 8. RESULTADO NEGATIVO (Não apto)
        // ==========================================
        composable("resultado_negativo") {
            ResultadoNegativoScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateHome = {
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                },
            )
        }

        // ==========================================
        // 9. AGENDAMENTO
        // ==========================================
        composable("agendamento") {
            AgendamentoScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateHome = {
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                },
                onNavigateToCheckin = {
                    navController.navigate("checkin") { popUpTo("home") }
                },
            )
        }

        // ==========================================
        // 10. EMERGÊNCIA
        // ==========================================
        composable("emergencia") {
            EmergenciaScreen(
                onNavigateBack = { navController.popBackStack() },
            ) { navController.navigate("agendamento") }
        }

        // ==========================================
        // 11. HISTÓRICO
        // ==========================================
        composable("historico") {
            HistoricoScreen { navController.popBackStack() }
        }

        // ==========================================
        // 12. ESTOQUE DE SANGUE
        // ==========================================
        composable("estoque") {
            EstoqueScreen(
                onNavigateBack = { navController.popBackStack() },
            ) { navController.navigate("posso_doar") }
        }

        // ==========================================
        // 13. HEMOCENTROS PRÓXIMOS
        // ==========================================
        composable("hemocentros") {
            HemocentrosScreen(
                onNavigateBack = { navController.popBackStack() },
            ) { navController.navigate("posso_doar") }
        }

        // ==========================================
        // 14. HOME HEMOCENTRO
        // ==========================================
        composable("hemo_home") {
            HemoHomeScreen(
                onNavigateToInicio = { /* Já está na home do hemo */ },
                onNavigateToAgenda = { navController.navigate("hemo_agenda") },
                onNavigateToEstoque = { navController.navigate("hemo_estoque") },
                onNavigateToChat = { navController.navigate("hemo_chat") },
                onNavigateToMais = { navController.navigate("hemo_mais") },
                onNavigateToCriarChamado = { navController.navigate("hemo_estoque") }
            )
        }

        // ==========================================
        // 15. TELAS DO HEMOCENTRO
        // ==========================================
        composable("hemo_agenda") {
            HemoAgendaScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToInicio = { navController.navigate("hemo_home") { popUpTo("hemo_home") } },
                onNavigateToAgenda = { /* Já está aqui */ },
                onNavigateToEstoque = { navController.navigate("hemo_estoque") },
                onNavigateToChat = { navController.navigate("hemo_chat") },
                onNavigateToMais = { navController.navigate("hemo_mais") },
                onNavigateToEscanear = { navController.navigate("hemo_scanner") }
            )
        }

        composable("hemo_scanner") {
            HemoScannerScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("hemo_estoque") {
            HemoEstoqueScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToInicio = { navController.navigate("hemo_home") { popUpTo("hemo_home") } },
                onNavigateToAgenda = { navController.navigate("hemo_agenda") },
                onNavigateToEstoque = { /* Já está aqui */ },
                onNavigateToChat = { navController.navigate("hemo_chat") },
                onNavigateToMais = { navController.navigate("hemo_mais") }
            )
        }

        composable("hemo_chat") {
            HemoChatScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToInicio = { navController.navigate("hemo_home") { popUpTo("hemo_home") } },
                onNavigateToAgenda = { navController.navigate("hemo_agenda") },
                onNavigateToEstoque = { navController.navigate("hemo_estoque") },
                onNavigateToChat = { /* Já está no chat */ },
                onNavigateToMais = { navController.navigate("hemo_mais") }
            )
        }

        composable("hemo_mais") {
            HemoMaisScreen(
                onNavigateToInicio = { navController.navigate("hemo_home") { popUpTo("hemo_home") } },
                onNavigateToAgenda = { navController.navigate("hemo_agenda") },
                onNavigateToEstoque = { navController.navigate("hemo_estoque") },
                onNavigateToChat = { navController.navigate("hemo_chat") },
                onNavigateToMais = { /* Já está aqui */ },
                onLogout = {
                    navController.navigate("landing_page") {
                        popUpTo(0) // Limpa tudo e volta para a tela inicial
                    }
                }
            )
        }
    }
}