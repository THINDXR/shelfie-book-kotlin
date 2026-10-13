package com.example.shelfie.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.shelfie.ui.screens.AuthScreen
import com.example.shelfie.ui.screens.BookDetailScreen
import com.example.shelfie.ui.screens.CartScreen
import com.example.shelfie.ui.screens.DiscoverScreen
import com.example.shelfie.ui.screens.LibraryScreen
import com.example.shelfie.ui.screens.ProfileScreen
import com.example.shelfie.ui.screens.ReaderScreen
import com.example.shelfie.ui.theme.BlushSoft
import com.example.shelfie.ui.theme.Cream
import com.example.shelfie.ui.theme.Ink
import com.example.shelfie.ui.theme.InkMuted
import com.example.shelfie.ui.theme.Maroon

/** Routes (screen addresses) used by the NavHost. */
object Routes {
    const val DISCOVER = "discover"
    const val CART = "cart"
    const val LIBRARY = "library"
    const val PROFILE = "profile"
    const val BOOK = "book/{key}"
    const val READER = "reader/{key}"
    fun book(key: String) = "book/$key"
    fun reader(key: String) = "reader/$key"
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.DISCOVER, "ค้นพบ", Icons.Outlined.Explore),
    Tab(Routes.CART, "ตะกร้า", Icons.Outlined.ShoppingBag),
    Tab(Routes.LIBRARY, "ชั้นหนังสือ", Icons.Outlined.AutoStories),
    Tab(Routes.PROFILE, "โปรไฟล์", Icons.Outlined.Person),
)

/** Page 1 is the sign-in screen; after signing in, the 4 tabs take over. */
@Composable
fun ShelfieApp(vm: AppViewModel) {
    val user by vm.user.collectAsStateWithLifecycle()
    if (user == null) {
        AuthScreen(vm)
    } else {
        MainScaffold(vm)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScaffold(vm: AppViewModel) {
    val nav = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val cart by vm.cart.collectAsStateWithLifecycle()
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    LaunchedEffect(Unit) {
        vm.messages.collect { message ->
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(message)
        }
    }

    Scaffold(
        containerColor = Cream,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (tabs.any { it.route == currentRoute }) {
                NavigationBar(containerColor = Color.White) {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = { nav.goToTab(tab.route) },
                            icon = {
                                if (tab.route == Routes.CART && cart.isNotEmpty()) {
                                    BadgedBox(badge = { Badge(containerColor = Maroon) { Text("${cart.size}") } }) {
                                        Icon(tab.icon, contentDescription = null)
                                    }
                                } else {
                                    Icon(tab.icon, contentDescription = null)
                                }
                            },
                            label = { Text(tab.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Maroon,
                                selectedTextColor = Maroon,
                                indicatorColor = BlushSoft,
                                unselectedIconColor = InkMuted,
                                unselectedTextColor = InkMuted,
                            ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Routes.DISCOVER,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.DISCOVER) {
                DiscoverScreen(
                    vm = vm,
                    onOpenBook = { nav.navigate(Routes.book(it)) },
                    onRead = { nav.navigate(Routes.reader(it)) },
                    onGoCart = { nav.goToTab(Routes.CART) },
                    onGoProfile = { nav.goToTab(Routes.PROFILE) },
                )
            }
            composable(Routes.CART) {
                CartScreen(
                    vm = vm,
                    onOpenBook = { nav.navigate(Routes.book(it)) },
                    onGoDiscover = { nav.goToTab(Routes.DISCOVER) },
                    onGoLibrary = { nav.goToTab(Routes.LIBRARY) },
                )
            }
            composable(Routes.LIBRARY) {
                LibraryScreen(
                    vm = vm,
                    onOpenBook = { nav.navigate(Routes.book(it)) },
                    onRead = { nav.navigate(Routes.reader(it)) },
                    onGoCart = { nav.goToTab(Routes.CART) },
                    onGoDiscover = { nav.goToTab(Routes.DISCOVER) },
                )
            }
            composable(Routes.PROFILE) {
                ProfileScreen(
                    vm = vm,
                    onOpenLibrary = { status ->
                        vm.setLibraryFilter(status)
                        nav.goToTab(Routes.LIBRARY)
                    },
                )
            }
            composable(
                Routes.BOOK,
                arguments = listOf(navArgument("key") { type = NavType.StringType }),
            ) { entry ->
                val key = entry.arguments?.getString("key").orEmpty()
                BookDetailScreen(
                    bookKey = key,
                    vm = vm,
                    onBack = { nav.popBackStack() },
                    onRead = { nav.navigate(Routes.reader(it)) },
                    onGoCart = { nav.goToTab(Routes.CART) },
                )
            }
            composable(
                Routes.READER,
                arguments = listOf(navArgument("key") { type = NavType.StringType }),
            ) { entry ->
                val key = entry.arguments?.getString("key").orEmpty()
                ReaderScreen(bookKey = key, vm = vm, onBack = { nav.popBackStack() })
            }
        }
    }
}

/** Switch bottom tabs without stacking copies of the same screen. */
private fun NavHostController.goToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
