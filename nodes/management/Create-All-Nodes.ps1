Param
(
    [string]$VMPassword
)

# Creates all the nodes that we use in the dotnet-ci Jenkins instance.

$VMUsername="dotnet-bot"
$VMService="dotnet-ci-nodes"

# Basic VM props
$TotalBasicWindowsBuildVMs=0
$BasicWindowsBuildImage="dci-win-bld-5"

echo "Creating $TotalBasicWindowsBuildVMs Windows Basic Build VMs (Basic_A3, image $BasicWindowsBuildImage)"

for ($i=1;$i -le $TotalBasicWindowsBuildVMs; $i++)
{
    New-AzureQuickVM -Windows -ServiceName $VMService -Name "dci-win-bld-$i" -ImageName $BasicWindowsBuildImage -AdminUsername $VMUsername -Password $VMPassword -InstanceSize Basic_A3
}

# Now for the "fast" windows machines

$TotalFastWindowsBuildVMs=10
$BasicWindowsBuildImage="dci-win-bld-5"

echo "Creating $TotalFastWindowsBuildVMs Windows Fast Build VMs (Standard_D3, image $BasicWindowsBuildImage)"

for ($i=1;$i -le $TotalFastWindowsBuildVMs; $i++)
{
    New-AzureQuickVM -Windows -ServiceName $VMService -Name "dci-win-fbld-$i" -ImageName $BasicWindowsBuildImage -AdminUsername $VMUsername -Password $VMPassword -InstanceSize Standard_D3
}