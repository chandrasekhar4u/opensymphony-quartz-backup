<html>
<body>
<h2><@s.text name="title.definitions.heading"/></h2>
<i><@s.text name="title.definitions.hint"/></i><br/>
<a href="edit.action"><@s.text name="label.create.definition"/></a>
<a href="raw.action"><@s.text name="label.dump"/></a>
<table cellspacing="0" cellpadding="5" width="100%" class="simple">
    <thead>
    <tr>
	<th><em><@s.text name="label.definitions.actions"/></em></th>
        <th><@s.text name="label.definitions.name"/></th>
        <th><@s.text name="label.definitions.description"/></th>
        <th><@s.text name="label.definitions.class"/></th>
        <th><@s.text name="label.definitions.params"/></th>      
   </tr>
   </thead>
    <#list definitions as def>
    <#if def_index % 2 == 0><tr class="odd"><#else><tr class="even"></#if>
        <td  nowrap="true"><a href="${base}/jobs/createDefinedJob.action?method=start&definitionName=${def.name!}"><@s.text name="label.global.createJob"/></a>
        <a href="edit.action?definitionName=${def.name!}"><@s.text name="label.global.edit"/></a>
        <a onclick="javascript:return confirm('<@s.text name="label.confirm.deleteDefinition"/>');" href="/delete.action?definitionName=${def.name!}"><@s.text name="label.global.delete"/></a>
        </td>
        <td> ${def.class} ${def.name!}</td>
        <td >${def.description!}</td>
        <td >${def.className!}</td>
        <td>
        <#if def.parameters?exists >
        <#assign params = def.parameters />
    	<#list params as p>
            <li>${p.name}: <@s.text name="label.global.required"/>=${p.required?string} </li>
     	</#list>
     	</#if>
        </td>
    </tr>
    </#list>
    </table>
</center>
</body>
</html>