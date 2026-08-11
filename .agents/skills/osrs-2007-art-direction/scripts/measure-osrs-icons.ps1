param(
	[Parameter(Mandatory = $true)]
	[string]$IconDirectory,

	[ValidateRange(1, 10000)]
	[int]$SampleSize = 500
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$analysisDirectory = (Resolve-Path -LiteralPath $IconDirectory).Path
$analysisFiles = @(Get-ChildItem -LiteralPath $analysisDirectory -Filter '*.png' -File |
	Sort-Object Name)
if ($analysisFiles.Count -eq 0)
{
	throw "No PNG files found in $analysisDirectory"
}

$analysisCount = [Math]::Min($SampleSize, $analysisFiles.Count)
$analysisSample = @($analysisFiles | Get-Random -Count $analysisCount -SetSeed 2007)
$analysisDimensions = @{}
$analysisColorCounts = New-Object System.Collections.Generic.List[int]
$analysisOccupancies = New-Object System.Collections.Generic.List[double]
$analysisSemiTransparentIcons = 0
$analysisNonEmptyIcons = 0

foreach ($analysisFile in $analysisSample)
{
	$analysisBitmap = [System.Drawing.Bitmap]::FromFile($analysisFile.FullName)
	try
	{
		$analysisDimension = "$($analysisBitmap.Width)x$($analysisBitmap.Height)"
		if ($analysisDimensions.ContainsKey($analysisDimension))
		{
			$analysisDimensions[$analysisDimension]++
		}
		else
		{
			$analysisDimensions[$analysisDimension] = 1
		}
		$analysisColors = [System.Collections.Generic.HashSet[int]]::new()
		$analysisOpaquePixels = 0
		$analysisHasSemiTransparentPixel = $false

		for ($analysisY = 0; $analysisY -lt $analysisBitmap.Height; $analysisY++)
		{
			for ($analysisX = 0; $analysisX -lt $analysisBitmap.Width; $analysisX++)
			{
				$analysisPixel = $analysisBitmap.GetPixel($analysisX, $analysisY)
				if ($analysisPixel.A -gt 0)
				{
					$analysisOpaquePixels++
					[void]$analysisColors.Add($analysisPixel.ToArgb())
				}
				if ($analysisPixel.A -gt 0 -and $analysisPixel.A -lt 255)
				{
					$analysisHasSemiTransparentPixel = $true
				}
			}
		}

		if ($analysisOpaquePixels -gt 0)
		{
			$analysisNonEmptyIcons++
		}
		if ($analysisHasSemiTransparentPixel)
		{
			$analysisSemiTransparentIcons++
		}
		$analysisColorCounts.Add($analysisColors.Count)
		$analysisOccupancies.Add($analysisOpaquePixels / ($analysisBitmap.Width * $analysisBitmap.Height))
	}
	finally
	{
		$analysisBitmap.Dispose()
	}
}

$analysisSortedColors = @($analysisColorCounts | Sort-Object)
$analysisSortedOccupancies = @($analysisOccupancies | Sort-Object)
function Get-AnalysisPercentile([array]$Values, [double]$Percentile)
{
	$analysisIndex = [Math]::Min($Values.Count - 1, [Math]::Floor(($Values.Count - 1) * $Percentile))
	return $Values[$analysisIndex]
}

[PSCustomObject]@{
	SampleSize = $analysisCount
	Dimensions = ($analysisDimensions.GetEnumerator() | Sort-Object Value -Descending |
		ForEach-Object { "$($_.Name):$($_.Value)" }) -join ', '
	NonEmptyIcons = $analysisNonEmptyIcons
	IconsWithSemiTransparentPixels = $analysisSemiTransparentIcons
	MedianOpaqueColors = Get-AnalysisPercentile $analysisSortedColors 0.50
	P90OpaqueColors = Get-AnalysisPercentile $analysisSortedColors 0.90
	MedianOccupancy = [Math]::Round((Get-AnalysisPercentile $analysisSortedOccupancies 0.50), 3)
}
