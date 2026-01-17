# Add project specific ProGuard rules here.
-keepclasseswithmembernames class * {
    native <methods>;
}

-keep class com.midibt.controller.synth.SynthEngine {
    native <methods>;
}
