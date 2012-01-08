function ()
{
	var form_num = 1,
		select_id = 'term_id',
		
		form = document.forms[form_num],
		select = document.getElementById(select_id);
	
	for (var i = 0; i < select.options.length; ++i) /* these are options of the dropdown holding the menu entries */
	{
		console.log('HERPDERP' +
			select.options[i].text +
					'DERPHERP' +
			'document.getElementById("' + select_id + '").selectedIndex='+i+';
			 document.forms[' + form_num + '].submit()' /* submit the form on ListView click */
		);
	}
}