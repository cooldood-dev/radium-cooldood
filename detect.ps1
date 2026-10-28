param(
    [switch]$FindJava,
    [switch]$Launch,
    [string]$ScriptDir,
    [string]$MinecraftDir,
    [string]$JavaExe,
    [string[]]$GameArgs = @()
)

if ($FindJava) {
    $candidates = [System.Collections.Generic.List[string]]::new()
    
    if ($env:JAVA_HOME) {
        $jh = Join-Path $env:JAVA_HOME 'bin\java.exe'
        if (Test-Path $jh) { $candidates.Add($jh) }
    }

    try {
        $pathJava = (Get-Command java -ErrorAction SilentlyContinue | Select-Object -ExpandProperty Source)
        if ($pathJava -and (Test-Path $pathJava)) { $candidates.Add($pathJava) }
    } catch {}

    $knownPaths = @(
        'C:\Program Files\Java\jdk1.8.0_202\bin\java.exe',
        'C:\Program Files\Java\jre1.8.0_202\bin\java.exe',
        (Join-Path $env:APPDATA '.minecraft\runtime\jre-legacy\windows-x64\jre-legacy\bin\java.exe'),
        (Join-Path $env:APPDATA '.minecraft\runtime\jre-legacy\windows-x86\jre-legacy\bin\java.exe')
    )
    foreach ($p in $knownPaths) {
        if ($p -and (Test-Path $p)) { $candidates.Add($p) }
    }

    $javaSearchDirs = @(
        'C:\Program Files\Java',
        'C:\Program Files (x86)\Java',
        'C:\Program Files\Eclipse Adoptium',
        'C:\Program Files\BellSoft',
        'C:\Program Files\Zulu',
        'C:\Program Files\Amazon Corretto'
    )
    foreach ($dir in $javaSearchDirs) {
        if (Test-Path $dir) {
            $found = Get-ChildItem -Path $dir -Recurse -Filter 'java.exe' -ErrorAction SilentlyContinue | Select-Object -ExpandProperty FullName
            foreach ($f in $found) {
                if ($f -and (Test-Path $f)) { $candidates.Add($f) }
            }
        }
    }

    $detected64 = $null
    $detected32 = $null
    $checked = [System.Collections.Generic.HashSet[string]]::new()
    foreach ($exe in $candidates) {
        if (-not $checked.Add($exe)) { continue }
        try {
            $ver = & $exe -version 2>&1 | Out-String
            if ($ver -match '1\.8\.' -or $ver -match 'version "8[\."]') {
                if ($ver -match '64-Bit' -or $ver -match 'x86_64' -or $ver -match 'amd64') {
                    $detected64 = $exe
                    break
                } elseif (-not $detected32) {
                    $detected32 = $exe
                }
            }
        } catch {}
    }

    $detected = if ($detected64) { $detected64 } else { $detected32 }
    if ($detected) {
        Write-Output $detected
    }
    exit 0
}

if ($Launch) {
    if (-not $ScriptDir) {
        $ScriptDir = $PSScriptRoot
    }
    if (-not $MinecraftDir) {
        $MinecraftDir = Join-Path $env:APPDATA '.minecraft'
    }

    # Locate prebuilt Radium.jar and Radium.json
    $jsonPath = $null
    $jarPath = $null

    $searchJson = @(
        (Join-Path $ScriptDir 'Radium.json'),
        (Join-Path $MinecraftDir 'versions\Radium\Radium.json')
    )
    foreach ($p in $searchJson) {
        if (Test-Path $p) { $jsonPath = $p; break }
    }

    $searchJar = @(
        (Join-Path $ScriptDir 'Radium.jar'),
        (Join-Path $ScriptDir 'build\libs\radium-1.4.0.jar'),
        (Join-Path $MinecraftDir 'versions\Radium\Radium.jar')
    )
    foreach ($p in $searchJar) {
        if (Test-Path $p) { $jarPath = $p; break }
    }

    if (-not $jsonPath -or -not (Test-Path $jsonPath)) {
        Write-Host "[-] Radium.json not found!" -ForegroundColor Red
        exit 1
    }
    if (-not $jarPath -or -not (Test-Path $jarPath)) {
        Write-Host "[-] Radium.jar not found!" -ForegroundColor Red
        exit 1
    }

    Write-Host "[+] Using Version JSON: $jsonPath" -ForegroundColor Cyan
    Write-Host "[+] Using Client JAR:   $jarPath" -ForegroundColor Cyan

    $versionData = Get-Content $jsonPath -Raw | ConvertFrom-Json
    $mainClass = if ($versionData.mainClass) { $versionData.mainClass } else { "net.minecraft.client.main.Main" }

    $nativesDir = Join-Path $ScriptDir 'natives'
    $mcLibsDir = Join-Path $MinecraftDir 'libraries'
    $localLibDir = Join-Path $ScriptDir 'lib'

    $cpElements = [System.Collections.Generic.List[string]]::new()
    $cpElements.Add($jarPath)

    # 1. Parse libraries from Radium.json
    if ($versionData.libraries) {
        foreach ($lib in $versionData.libraries) {
            $name = $lib.name
            if (-not $name) { continue }
            $parts = $name.Split(':')
            if ($parts.Length -lt 3) { continue }

            $group = $parts[0].Replace('.', '\')
            $artifact = $parts[1]
            $version = $parts[2]
            $classifier = if ($parts.Length -ge 4) { "-" + $parts[3] } else { "" }

            $jarFileName = "$artifact-$version$classifier.jar"
            $mcLibPath = Join-Path $mcLibsDir "$group\$artifact\$version\$jarFileName"

            if (Test-Path $mcLibPath) {
                if (-not $cpElements.Contains($mcLibPath)) {
                    $cpElements.Add($mcLibPath)
                }
            } else {
                # Fallback to local project lib folder
                $localFound = Get-ChildItem -Path $localLibDir -Recurse -Filter $jarFileName -ErrorAction SilentlyContinue | Select-Object -ExpandProperty FullName -First 1
                if ($localFound -and -not $cpElements.Contains($localFound)) {
                    $cpElements.Add($localFound)
                }
            }
        }
    }

    # 2. Add local jars (e.g. okhttp, okio, etc.)
    if (Test-Path $localLibDir) {
        $localJars = Get-ChildItem -Path $localLibDir -Recurse -Filter '*.jar' -ErrorAction SilentlyContinue | Select-Object -ExpandProperty FullName
        foreach ($lj in $localJars) {
            if (-not $cpElements.Contains($lj)) {
                $cpElements.Add($lj)
            }
        }
    }

    # 3. Add game arguments
    $runDir = Join-Path $ScriptDir 'run'
    if (-not (Test-Path $runDir)) {
        New-Item -ItemType Directory -Path $runDir -Force | Out-Null
    }

    # Detect if Java runtime is 32-bit or 64-bit to set safe heap limit
    $maxHeap = "2048M"
    try {
        $ver = & $JavaExe -version 2>&1 | Out-String
        if ($ver -notmatch '64-Bit' -and $ver -notmatch 'x86_64' -and $ver -notmatch 'amd64') {
            $maxHeap = "1024M"
        }
    } catch {}

    $arguments = @(
        '-Xms512M',
        "-Xmx$maxHeap",
        "-Djava.library.path=$nativesDir",
        '-cp',
        ($cpElements -join ';'),
        $mainClass
    )

    if ($versionData.minecraftArguments) {
        $assetsDir = Join-Path $MinecraftDir 'assets'
        $assetIndex = if ($versionData.assetIndex -and $versionData.assetIndex.id) { $versionData.assetIndex.id } else { "1.8" }
        
        $mcArgsTemplate = $versionData.minecraftArguments
        $mcArgsTemplate = $mcArgsTemplate.Replace('${auth_player_name}', 'Player')
        $mcArgsTemplate = $mcArgsTemplate.Replace('${version_name}', 'Radium')
        $mcArgsTemplate = $mcArgsTemplate.Replace('${game_directory}', $runDir)
        $mcArgsTemplate = $mcArgsTemplate.Replace('${assets_root}', $assetsDir)
        $mcArgsTemplate = $mcArgsTemplate.Replace('${assets_index_name}', $assetIndex)
        $mcArgsTemplate = $mcArgsTemplate.Replace('${auth_uuid}', '00000000-0000-0000-0000-000000000000')
        $mcArgsTemplate = $mcArgsTemplate.Replace('${auth_access_token}', '0')
        $mcArgsTemplate = $mcArgsTemplate.Replace('${user_properties}', '{}')
        $mcArgsTemplate = $mcArgsTemplate.Replace('${user_type}', 'mojang')

        $splitArgs = -split $mcArgsTemplate
        $arguments += $splitArgs
    }

    if ($GameArgs) {
        $arguments += $GameArgs
    }

    # Build properly quoted command-line argument string for Windows process execution
    $quotedArgs = [System.Collections.Generic.List[string]]::new()
    foreach ($arg in $arguments) {
        if ($arg -match '[\s"]') {
            # Escape inner double quotes and wrap in quotes
            $escaped = $arg -replace '(\\*)(")', '$1$1\"'
            $escaped = $escaped -replace '(\\+)$', '$1$1'
            $quotedArgs.Add("`"$escaped`"")
        } else {
            $quotedArgs.Add($arg)
        }
    }

    Set-Location $runDir
    $psi = [System.Diagnostics.ProcessStartInfo]::new()
    $psi.FileName = $JavaExe
    $psi.Arguments = ($quotedArgs -join ' ')
    $psi.WorkingDirectory = $runDir
    $psi.UseShellExecute = $false

    $proc = [System.Diagnostics.Process]::Start($psi)
    $proc.WaitForExit()
    exit $proc.ExitCode
}
