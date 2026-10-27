package com.example.dsy1105_reservas.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.dsy1105_reservas.Sesion
import com.example.dsy1105_reservas.TicketViewModel
import com.example.dsy1105_reservas.model.Rol
import com.example.dsy1105_reservas.model.Usuario
import com.example.dsy1105_reservas.ui.AprobacionesScreen
import com.example.dsy1105_reservas.ui.ArchivosScreen
import com.example.dsy1105_reservas.ui.CerrarSesionScreen
import com.example.dsy1105_reservas.ui.EventosScreen
import com.example.dsy1105_reservas.ui.HorarioScreen
import com.example.dsy1105_reservas.ui.LoginScreen
import com.example.dsy1105_reservas.ui.RegistroScreen
import com.example.dsy1105_reservas.ui.RequerimientosScreen
import com.example.dsy1105_reservas.ui.SolicitarEspacioScreen
import com.example.dsy1105_reservas.ui.VisorArchivoScreen
import kotlinx.coroutines.launch

object Rutas {
    const val LOGIN = "login"
    const val REGISTRO = "registro"
    const val EVENTOS = "eventos"
    const val REQUERIMIENTOS = "requerimientos"
    const val SOLICITAR = "solicitar"
    const val APROBACIONES = "aprobaciones"
    const val HORARIO = "horario"
    const val ARCHIVOS = "archivos"
    const val CERRAR_SESION = "cerrar_sesion"
    const val VISOR = "visor/{id}"

    fun visor(id: Long) = "visor/$id"
}

/** Destinos principales y roles habilitados. */
enum class Destino(
    val ruta: String,
    val etiqueta: String,
    val icono: ImageVector,
    val roles: Set<Rol>
) {
    EVENTOS(Rutas.EVENTOS, "Eventos", Icons.Filled.Home, Rol.entries.toSet()),
    REQUERIMIENTOS(Rutas.REQUERIMIENTOS, "Requerimientos", Icons.AutoMirrored.Filled.List, Rol.entries.toSet()),
    SOLICITAR(Rutas.SOLICITAR, "Solicitar espacio", Icons.Filled.Add, setOf(Rol.DOCENTE, Rol.ENCARGADO)),
    APROBACIONES(Rutas.APROBACIONES, "Aprobaciones", Icons.Filled.Check, setOf(Rol.ADMINISTRADOR)),
    HORARIO(Rutas.HORARIO, "Horarios", Icons.Filled.DateRange, Rol.entries.toSet()),
    ARCHIVOS(Rutas.ARCHIVOS, "Archivos", Icons.Filled.Description, Rol.entries.toSet())
}

@Composable
fun MpazTicketApp(vm: TicketViewModel = viewModel()) {
    val sesion by vm.sesion.collectAsState()
    when (val actual = sesion) {
        Sesion.Cargando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        else -> Contenido(vm, (actual as? Sesion.Activa)?.usuario)
    }
}

private fun NavHostController.irA(ruta: String) {
    navigate(ruta) {
        popUpTo(Rutas.EVENTOS) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private fun tituloDe(ruta: String?): String =
    Destino.entries.firstOrNull { it.ruta == ruta }?.etiqueta
        ?: if (ruta == Rutas.CERRAR_SESION) "Cerrar sesión" else "MPAZ Ticket"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Contenido(vm: TicketViewModel, usuario: Usuario?) {
    val navController = rememberNavController()
    val entrada by navController.currentBackStackEntryAsState()
    val rutaActual = entrada?.destination?.route
    val inicio = remember { if (usuario != null) Rutas.EVENTOS else Rutas.LOGIN }

    // Cuando cambia la sesión, se limpia la pila y se lleva a la persona a la vista que corresponde.
    LaunchedEffect(usuario?.id) {
        val ruta = navController.currentDestination?.route ?: return@LaunchedEffect
        val enAuth = ruta == Rutas.LOGIN || ruta == Rutas.REGISTRO
        if (usuario == null && !enAuth) {
            navController.navigate(Rutas.LOGIN) {
                popUpTo(navController.graph.id) { inclusive = true }
                launchSingleTop = true
            }
        } else if (usuario != null && enAuth) {
            navController.navigate(Rutas.EVENTOS) {
                popUpTo(navController.graph.id) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    val destinosVisibles = Destino.entries.filter { usuario != null && usuario.rol in it.roles }
    val esDestinoPrincipal = destinosVisibles.any { it.ruta == rutaActual }
    val mostrarMarco = usuario != null && (esDestinoPrincipal || rutaActual == Rutas.CERRAR_SESION)

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = mostrarMarco,
        drawerContent = {
            if (usuario != null) {
                MenuLateral(
                    usuario = usuario,
                    destinos = destinosVisibles,
                    rutaActual = rutaActual,
                    onDestino = { ruta ->
                        scope.launch { drawerState.close() }
                        navController.irA(ruta)
                    },
                    onCerrarSesion = {
                        scope.launch { drawerState.close() }
                        navController.navigate(Rutas.CERRAR_SESION) { launchSingleTop = true }
                    }
                )
            }
        }
    ) {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                AnimatedVisibility(
                    visible = mostrarMarco,
                    enter = fadeIn(tween(250)) + slideInVertically(tween(250)) { -it },
                    exit = fadeOut(tween(150)) + slideOutVertically(tween(150)) { -it }
                ) {
                    CenterAlignedTopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            titleContentColor = MaterialTheme.colorScheme.primary,
                        ),
                        title = {
                            Text(tituloDe(rutaActual), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Filled.Menu, contentDescription = "Abrir menú")
                            }
                        },
                        actions = {
                            IconButton(onClick = {
                                navController.navigate(Rutas.CERRAR_SESION) { launchSingleTop = true }
                            }) {
                                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Cerrar sesión")
                            }
                        },
                        scrollBehavior = scrollBehavior
                    )
                }
            },
            bottomBar = {
                AnimatedVisibility(
                    visible = mostrarMarco && esDestinoPrincipal,
                    enter = fadeIn(tween(250)) + slideInVertically(tween(250)) { it },
                    exit = fadeOut(tween(150)) + slideOutVertically(tween(150)) { it }
                ) {
                    NavigationBar {
                        destinosVisibles.forEach { destino ->
                            NavigationBarItem(
                                selected = rutaActual == destino.ruta,
                                onClick = { navController.irA(destino.ruta) },
                                icon = { Icon(destino.icono, contentDescription = destino.etiqueta) },
                                label = { Text(destino.etiqueta, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                // Con 5 destinos en pantallas chicas solo se muestra la etiqueta del seleccionado.
                                alwaysShowLabel = false
                            )
                        }
                    }
                }
            }
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = inicio,
                modifier = Modifier.fillMaxSize(),
                enterTransition = { fadeIn(tween(300)) + slideInHorizontally(tween(300)) { it / 8 } },
                exitTransition = { fadeOut(tween(200)) },
                popEnterTransition = { fadeIn(tween(300)) },
                popExitTransition = { fadeOut(tween(200)) + slideOutHorizontally(tween(300)) { it / 8 } }
            ) {
                composable(Rutas.LOGIN) {
                    LoginScreen(vm, padding, onIrARegistro = { navController.navigate(Rutas.REGISTRO) })
                }
                composable(Rutas.REGISTRO) {
                    RegistroScreen(vm, padding, onVolver = { navController.popBackStack() })
                }
                composable(Rutas.EVENTOS) { EventosScreen(vm, padding) }
                composable(Rutas.REQUERIMIENTOS) { RequerimientosScreen(vm, padding) }
                composable(Rutas.SOLICITAR) { SolicitarEspacioScreen(vm, padding) }
                composable(Rutas.APROBACIONES) { AprobacionesScreen(vm, padding) }
                composable(Rutas.HORARIO) { HorarioScreen(vm, padding) }
                composable(Rutas.ARCHIVOS) {
                    ArchivosScreen(vm, padding, onVer = { id -> navController.navigate(Rutas.visor(id)) })
                }
                composable(
                    route = Rutas.VISOR,
                    arguments = listOf(navArgument("id") { type = NavType.LongType })
                ) { backStackEntry ->
                    VisorArchivoScreen(
                        vm = vm,
                        archivoId = backStackEntry.arguments?.getLong("id") ?: -1L,
                        onVolver = { navController.popBackStack() }
                    )
                }
                composable(Rutas.CERRAR_SESION) {
                    CerrarSesionScreen(vm, padding, onCancelar = { navController.popBackStack() })
                }
            }
        }
    }
}

@Composable
private fun MenuLateral(
    usuario: Usuario,
    destinos: List<Destino>,
    rutaActual: String?,
    onDestino: (String) -> Unit,
    onCerrarSesion: () -> Unit
) {
    ModalDrawerSheet {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("MPAZ Ticket", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                Text(usuario.nombre, style = MaterialTheme.typography.titleMedium)
                Text(usuario.correo, style = MaterialTheme.typography.bodySmall)
                Text(usuario.rol.etiqueta, style = MaterialTheme.typography.labelMedium)
            }
            HorizontalDivider()
            destinos.forEach { destino ->
                NavigationDrawerItem(
                    label = { Text(destino.etiqueta) },
                    icon = { Icon(destino.icono, contentDescription = null) },
                    selected = rutaActual == destino.ruta,
                    onClick = { onDestino(destino.ruta) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
            HorizontalDivider()
            NavigationDrawerItem(
                label = { Text("Cerrar sesión") },
                icon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null) },
                selected = rutaActual == Rutas.CERRAR_SESION,
                onClick = onCerrarSesion,
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
        }
    }
}
