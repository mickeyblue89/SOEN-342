$src = Get-ChildItem -Path 'Code/src/main/java' -Recurse -Filter *.java | ForEach-Object { $_.FullName }
$cp = @(
  "$env:USERPROFILE/.m2/repository/org/mnode/ical4j/ical4j/3.2.5/ical4j-3.2.5.jar",
  "$env:USERPROFILE/.m2/repository/com/mysql/mysql-connector-j/9.6.0/mysql-connector-j-9.6.0.jar",
  "$env:USERPROFILE/.m2/repository/org/slf4j/slf4j-simple/2.0.13/slf4j-simple-2.0.13.jar"
) -join ';'

New-Item -ItemType Directory -Force -Path 'Code/target/classes' | Out-Null
javac -cp $cp -d 'Code/target/classes' $src

if ($LASTEXITCODE -ne 0) {
  exit $LASTEXITCODE
}
