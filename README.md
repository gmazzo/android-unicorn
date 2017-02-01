# android-unicorn
A funny Unicorn widget to include in your layouts as a gadget

## Import
In `build.gradle` add:
```
    repositories {
        maven { url "https://dl.bintray.com/gmazzo/maven/" }
    }

    dependencies {
        compile 'com.github.gmazzo:unicorn:0.1'
    }
```

##Usage
```xml
    <com.github.gmazzo.unicorn.UnicornView
        android:layout_width="match_parent"
        android:layout_height="wrap_content" />
```
