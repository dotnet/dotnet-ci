Param
(
    [string]$VMPassword
)

# Creates all the nodes that we use in the dotnet-ci Jenkins instance.

$VMUsername="dotnet-bot"
$VMService="dotnet-ci-pool"

# Basic VM props
$TotalBasicWindowsBuildVMs=8
$BasicWindowsBuildImage="dci-win-bld-3"

echo "Creating $TotalBasicWindowsBuildVMs Windows Basic Build VMs (Basic_A3, image $BasicWindowsBuildImage)"

for ($i=1;$i -le $TotalBasicWindowsBuildVMs; $i++)
{
    New-AzureQuickVM -Windows -ServiceName $VMService -Name "dci-win-bld-$i" -ImageName $BasicWindowsBuildImage -AdminUsername $VMUsername -Password $VMPassword -InstanceSize Basic_A3
}

# Now for the "fast" windows machines

$TotalFastWindowsBuildVMs=4
$BasicWindowsBuildImage="dci-win-bld-3"

echo "Creating $TotalFastWindowsBuildVMs Windows Fast Build VMs (Basic_A3, image $BasicWindowsBuildImage)"

for ($i=1;$i -le $TotalFastWindowsBuildVMs; $i++)
{
    New-AzureQuickVM -Windows -ServiceName "dotnet-ci-nodes" -Name "dci-win-fbld-$i" -ImageName $BasicWindowsBuildImage -AdminUsername $VMUsername -Password $VMPassword -InstanceSize Standard_D3
}