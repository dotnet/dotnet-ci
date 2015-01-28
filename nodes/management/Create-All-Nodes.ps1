Param
(
    [string]$VMPassword
)

# Creates all the nodes that we use in the dotnet-ci Jenkins instance.

$VMUsername="dotnet-bot"
$VMService="dotnet-ci-pool"

# Basic VM props
$TotalBasicWindowsBuildVMs=5
$BasicWindowsBuildImage="dci-win-bld-3"

echo "Creating $TotalBasicWindowsBuildVMs Windows Basic Build VMs (Basic_A3, image $BasicWindowsBuildImage)"

for ($i=1;$i -le $TotalBasicWindowsBuildVMs; $i++)
{
    New-AzureQuickVM -Windows -ServiceName $VMService -Name "dci-win-bld-$i" -ImageName $BasicWindowsBuildImage -AdminUsername $VMUsername -Password $VMPassword -InstanceSize Basic_A3
}