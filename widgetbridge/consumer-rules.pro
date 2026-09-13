# Shipped inside the AAR as consumer keep rules, so R8 applies them to every app
# that depends on WidgetBridge without the app knowing about them.
#
# Glance renders every widget through a WorkManager CoroutineWorker, and WorkManager
# reaches two classes only by reflection (Class.forName(name).newInstance()):
#
# 1. The request's InputMerger; OneTimeWorkRequest defaults to OverwritingInputMerger.
#    work-runtime up to 2.9.x ships `-keep class * extends androidx.work.InputMerger`,
#    which under R8 full mode keeps the class but drops its no-arg constructor.
#    WorkerWrapper then fails with "Could not create Input Merger" before the worker
#    runs, and the widget stays on its initial loading layout forever, on release
#    builds only. work-runtime 2.10 fixed its own rule; Glance 1.1.1 resolves 2.7.1.
# 2. Its Room database, WorkDatabase_Impl, created through androidx.startup at process
#    start. room-runtime through 2.6.x ships `-keep class * extends androidx.room.RoomDatabase`,
#    same shape, same loss: "Failed to create an instance of androidx.work.impl.WorkDatabase"
#    crashes the app on launch.
#
# Both rules match nothing in an app without WorkManager and are no-ops where the
# libraries already keep the constructor.
-keep class * extends androidx.work.InputMerger { <init>(); }
-keep class * extends androidx.room.RoomDatabase { <init>(); }
