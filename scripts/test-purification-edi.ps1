$ErrorActionPreference = 'Stop'
$moduleRoot = Split-Path -Parent $PSScriptRoot
$javaRoot = if ($env:JAVA_HOME) { $env:JAVA_HOME } else { 'C:\Users\Ex_Je\.jdks\ms-21.0.11' }
$testOutput = Join-Path $moduleRoot 'build/purification-edi-test'
New-Item -ItemType Directory -Force -Path $testOutput | Out-Null
$packagePath = 'org/satou/gtecore/common/machine/multiblock/water'
& (Join-Path $javaRoot 'bin/javac.exe') --release 21 -d $testOutput `
    (Join-Path $moduleRoot "src/main/java/$packagePath/PurificationEdiState.java") `
    (Join-Path $moduleRoot "src/test/java/$packagePath/PurificationEdiStateTest.java")
if ($LASTEXITCODE -ne 0) { throw 'Purification EDI model compilation failed' }
& (Join-Path $javaRoot 'bin/java.exe') -cp $testOutput org.satou.gtecore.common.machine.multiblock.water.PurificationEdiStateTest
if ($LASTEXITCODE -ne 0) { throw 'Purification EDI model regression tests failed' }
