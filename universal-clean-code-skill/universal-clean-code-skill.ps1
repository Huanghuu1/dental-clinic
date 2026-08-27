$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
python "$Root\scripts\run_pipeline.py" @args
exit $LASTEXITCODE
