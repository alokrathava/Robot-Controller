# ProGuard / R8 rules for Robot SDK library release builds

-keepattributes LineNumberTable,SourceFile
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-renamesourcefileattribute SourceFile

# Include consumer rules for SDK release builds
-include consumer-rules.pro
