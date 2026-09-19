$ErrorActionPreference = 'Stop'
$moduleRoot = Split-Path -Parent $PSScriptRoot
$javaRoot = if ($env:JAVA_HOME) { $env:JAVA_HOME } else { 'C:\Users\Ex_Je\.jdks\ms-21.0.11' }
$testOutput = Join-Path $moduleRoot 'build/purification-uv-test'
New-Item -ItemType Directory -Force -Path $testOutput | Out-Null
$packagePath = 'org/satou/gtecore/common/machine/multiblock/water'
& (Join-Path $javaRoot 'bin/javac.exe') --release 21 -d $testOutput `
    (Join-Path $moduleRoot "src/main/java/$packagePath/PurificationUvState.java") `
    (Join-Path $moduleRoot "src/test/java/$packagePath/PurificationUvStateTest.java")
if ($LASTEXITCODE -ne 0) { throw 'Purification UV model compilation failed' }
& (Join-Path $javaRoot 'bin/java.exe') -cp $testOutput org.satou.gtecore.common.machine.multiblock.water.PurificationUvStateTest
if ($LASTEXITCODE -ne 0) { throw 'Purification UV model regression tests failed' }
