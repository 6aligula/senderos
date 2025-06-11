# Senderos

This project collects location updates and uploads them to a backend.

## Background location service

A new foreground service named `LocationSenderService` can be started from the map screen. The service keeps requesting GPS updates using the `FusedLocationProviderClient` and schedules a `LocationSenderWorker` for each new position. The service displays a persistent notification so the system keeps it alive in the foreground.

To allow background updates you must grant the `ACCESS_BACKGROUND_LOCATION` permission. The manifest already declares this permission.

Use the button labelled **Iniciar servicio** in the map screen to start sending locations even when the app is in the background. Tap **Detener servicio** to stop it.
