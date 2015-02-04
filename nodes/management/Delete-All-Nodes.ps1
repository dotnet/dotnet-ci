# Creates all the nodes that we use in the dotnet-ci Jenkins instance.

$ServiceName="dotnet-ci-pool"

# Basic VM props
$TotalBasicWindowsBuildVMs=0

echo "Deleting $TotalBasicWindowsBuildVMs Windows Basic Build VMs"

for ($i=1;$i -le $TotalBasicWindowsBuildVMs; $i++)
{
    Remove-AzureVM -ServiceName $ServiceName -Name "dci-win-bld-$i" -DeleteVHD
}

#Service name for these is different.

$ServiceName="dotnet-ci-nodes"
$TotalFastWindowsBuildVMs=10

echo "Deleting $TotalFastWindowsBuildVMs Windows Fast Build VMs"

for ($i=1;$i -le $TotalFastWindowsBuildVMs; $i++)
{
    Remove-AzureVM -ServiceName $ServiceName -Name "dci-win-fbld-$i" -DeleteVHD
}

#Service name for these is different.

$TotalFastLinuxBuildVMs=2

echo "Deleting $TotalFastLinuxBuildVMs Windows Fast Build VMs"

for ($i=1;$i -le $TotalFastLinuxBuildVMs; $i++)
{
    Remove-AzureVM -ServiceName $ServiceName -Name "dci-ub-fbld-$i" -DeleteVHD
}