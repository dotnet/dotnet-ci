# Creates all the nodes that we use in the dotnet-ci Jenkins instance.

$ServiceName="dotnet-ci-pool"

# Basic VM props
$TotalBasicWindowsBuildVMs=5

echo "Deleting $TotalBasicWindowsBuildVMs Windows Basic Build VMs"

for ($i=1;$i -le $TotalBasicWindowsBuildVMs; $i++)
{
    Remove-AzureVM -ServiceName $ServiceName -Name "dci-win-bld-$i" -DeleteVHD
}