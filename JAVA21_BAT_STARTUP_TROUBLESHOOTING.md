# Java 21 啟動排錯說明

適用情境：

- 公司環境把 `JAVA_HOME` 綁死
- `.bat` 啟動檔會吃到錯的 `java.exe`
- 專案需要使用 Java 21，但執行時跑到其他版本

## 先確認目前吃到哪個 Java

在 `cmd` 執行：

```bat
echo %JAVA_HOME%
where java
java -version
```

理想狀態：

- `JAVA_HOME` 指向 Java 21
- `where java` 第一個結果是 Java 21 的 `bin\java.exe`
- `java -version` 顯示 `21`

## 解法 1：只在目前視窗暫時覆蓋 JAVA_HOME

這是最常用也最安全的方式，不會改動公司電腦的全域設定。

```bat
set "JAVA_HOME=C:\Program Files\Java\jdk-21"
set "PATH=%JAVA_HOME%\bin;%PATH%"
java -version
scripts\start-project-navigator.bat
```

說明：

- 只影響目前這個 `cmd` 視窗
- 關掉視窗後就恢復原狀

## 解法 2：直接指定 Java 21 的完整路徑

如果 `.bat` 仍然被公司環境覆蓋，直接指定完整路徑最穩定。

```bat
"C:\Program Files\Java\jdk-21\bin\java.exe" -version
"C:\Program Files\Java\jdk-21\bin\java.exe" -jar target\project-navigator-0.1.0-SNAPSHOT.jar
```

說明：

- 不依賴 `JAVA_HOME`
- 不依賴 `PATH`
- 最不容易被其他系統設定干擾

## 解法 3：改使用者層級的環境變數

如果公司政策允許調整「使用者變數」，可以設定：

- 使用者變數 `JAVA_HOME` = `C:\Program Files\Java\jdk-21`
- 使用者變數 `Path` 把 `%JAVA_HOME%\bin` 放在前面

設定完成後，重新開新的 `cmd` 再檢查：

```bat
echo %JAVA_HOME%
where java
java -version
```

## 如果 `.bat` 檔優先讀 `%JAVA_HOME%\bin\java.exe`

那通常只要在執行前先做：

```bat
set "JAVA_HOME=C:\Program Files\Java\jdk-21"
set "PATH=%JAVA_HOME%\bin;%PATH%"
scripts\start-project-navigator.bat
```

就可以讓 `.bat` 吃到正確的 Java 21。

## 建議優先順序

1. 先用目前視窗暫時覆蓋 `JAVA_HOME`
2. 如果仍被蓋掉，就直接指定完整路徑的 `java.exe`
3. 如果公司允許，再考慮改使用者層級環境變數

## 補充

如果你的 `.bat` 是用下面這種邏輯：

```bat
%JAVA_HOME%\bin\java.exe
```

那代表它會高度依賴 `JAVA_HOME`。  
如果是用下面這種：

```bat
java
```

那就會依賴 `PATH` 中排在最前面的 Java。
