package com.sangue.sangue

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun AppNavigation() {
    // Controlador central que guarda o histórico de telas
    val navController = rememberNavController()

    // 🔥 O aplicativo agora inicia obrigatoriamente pela tela de Login/Cadastro
    NavHost(navController = navController, startDestination = "auth") {

        // ==========================================
        // 0. AUTENTICAÇÃO E CADASTRO
        // ==========================================
        composable("auth") {
            AuthScreen {
                // Após logar ou criar conta, vai para a Landing Page e limpa a pilha de login
                navController.navigate("landing_page") {
                    popUpTo("auth") { inclusive = true }
                }
            }
        }

        // ==========================================
        // 1. LANDING PAGE
        // ==========================================
        composable("landing_page") {
            LandPageScreen(
                onNavigateToHome = { navController.navigate("home") },
                onNavigateToCheckin = { navController.navigate("checkin") },
                onNavigateToEmergencia = { navController.navigate("emergencia") },
                onNavigateToHistorico = { navController.navigate("historico") },
                onNavigateToPerfil = {
                    navController.navigate("perfil")
                },
            )
        }

        // ==========================================
        // 2. HOME DASHBOARD
        // ==========================================
        composable("home") {
            HomeScreen(
                onNavigateToInicio = { navController.popBackStack() }, // Volta para a Landing Page
                onNavigateToVerificacao = { navController.navigate("posso_doar") }, // Botão Central
                onNavigateToCheckin = { navController.navigate("checkin") },        // Footer: Agendar
                onNavigateToEmergencia = { navController.navigate("emergencia") },  // Footer: Emergência
                onNavigateToHistorico = { navController.navigate("historico") },    // Footer: Histórico
                onNavigateToPerfil = { navController.navigate("perfil") },          // Footer: Perfil
                onNavigateToEstoque = {
                    navController.navigate("estoque")
                }, // Botão Ação Rápida: Estoque
            ) {
                navController.navigate("hemocentros") // Botão Ação Rápida: Hemocentros Próximos
            }
        }

        // ==========================================
        // 3. CHECK-IN
        // ==========================================
        composable("checkin") {
            CheckInScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateHome = {
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true } // Limpa a pilha e vai pra home
                    }
                },
            )
        }

        // ==========================================
        // 4. PERFIL (Com a Carteira, Edição, Configurações, etc.)
        // ==========================================
        composable("perfil") {
            PerfilScreen(
                onNavigateBack = { navController.popBackStack() },
                onLogout = {
                    // Limpa toda a pilha do aplicativo (popUpTo(0)) e volta pro Login
                    navController.navigate("auth") {
                        popUpTo(0)
                    }
                },
                onNavigateToTriagem = {
                    navController.navigate("posso_doar")
                }, // Botão da central de ajuda
            )
        }

        // ==========================================
        // 5. CONQUISTAS
        // ==========================================
        composable("conquistas") {
            ConquistasScreen {
                navController.popBackStack()
            }
        }

        // ==========================================
        // 6. POSSO DOAR? (Triagem)
        // ==========================================
        composable("posso_doar") {
            PossoDoarScreen(
                onNavigateToResultado = { navController.navigate("resultado") },
            ) {
                navController.popBackStack()
            }
        }

        // ==========================================
        // 7. RESULTADO (Apto a doar)
        // ==========================================
        composable("resultado") {
            ResultadoScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAgendamento = { navController.navigate("agendamento") },
            ) {
                navController.navigate("hemocentros")
            }
        }

        // ==========================================
        // 8. RESULTADO NEGATIVO (Não apto)
        // ==========================================
        composable("resultado_negativo") {
            ResultadoNegativoScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateHome = {
                    navController.navigate("landing_page") {
                        popUpTo("landing_page") { inclusive = true }
                    }
                },
            )
        }

        // ==========================================
        // 9. AGENDAMENTO (3 Etapas)
        // ==========================================
        composable("agendamento") {
            AgendamentoScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateHome = {
                    navController.navigate("landing_page") {
                        popUpTo("landing_page") { inclusive = true }
                    }
                },
                onNavigateToCheckin = {
                    // Direciona para o Check-in e mantém a Landing Page no fundo
                    navController.navigate("checkin") {
                        popUpTo("landing_page")
                    }
                },
            )
        }

        // ==========================================
        // 10. EMERGÊNCIA
        // ==========================================
        composable("emergencia") {
            EmergenciaScreen(
                onNavigateBack = { navController.popBackStack() },
            ) {
                navController.navigate("agendamento")
            }
        }

        // ==========================================
        // 11. HISTÓRICO
        // ==========================================
        composable("historico") {
            HistoricoScreen {
                navController.popBackStack()
            }
        }

        // ==========================================
        // 12. ESTOQUE DE SANGUE
        // ==========================================
        composable("estoque") {
            EstoqueScreen(
                onNavigateBack = { navController.popBackStack() },
            ) {
                navController.navigate("posso_doar")
            }
        }

        // ==========================================
        // 13. HEMOCENTROS PRÓXIMOS
        // ==========================================
        composable("hemocentros") {
            HemocentrosScreen(
                onNavigateBack = { navController.popBackStack() },
            ) {
                navController.navigate("posso_doar")
            }
        }
    }
}
