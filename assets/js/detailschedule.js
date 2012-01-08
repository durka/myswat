function ()
{
	var tables = document.getElementsByClassName('datadisplaytable'),
		classes = [];
	
	for (var i = 0; i < tables.length; ++i) /* there is a pair of tables for each class */
	{
		var klass = tables[i].caption.innerText.split(/ ?- ?/);
		var name = klass[0];
		if (i+1 <= tables.length && tables[i+1].summary.substr(0, 10) == 'This table')
		{
			if (tables[i+1].rows[1].cells[5].innerText == 'Lab')
			{
				classes[classes.length-1].time += ', lab ' + tables[i+1].rows[1].cells[2].innerText + ' ' + tables[i+1].rows[1].cells[1].innerText.split(" - ")[0];
				classes[classes.length-1].place += ', lab ' + tables[i+1].rows[1].cells[3].innerText;
			}
			else if (tables[i+1].rows[1].cells[5].innerText == 'Problem session')
			{
				classes[classes.length-1].time += ', prob sess ' + tables[i+1].rows[1].cells[2].innerText + ' ' + tables[i+1].rows[1].cells[1].innerText.split(" - ")[0];
				classes[classes.length-1].place += ', prob sess ' + tables[i+1].rows[1].cells[3].innerText;
			}
			else
			{
				classes[classes.length] = {
					'name': name,
					'section': klass[1] + ' (' + klass[2] + ')',
					'prof': tables[i+1].rows[1].cells[6].innerText,
					'time': tables[i+1].rows[1].cells[2].innerText + ' ' + tables[i+1].rows[1].cells[1].innerText.split(" - ")[0],
					'place': tables[i+1].rows[1].cells[3].innerText,
					'credit': Math.round(tables[i].rows[5].cells[1].innerText*10)/10 + ' credit(s), ' + tables[i].rows[4].cells[1].innerText,
				};
			}
			++i;
		}
		else
		{
			classes[classes.length] = {
				'name': name,
				'section': klass[1] + ' (' + klass[2] + ')',
				'prof': '',
				'time': '',
				'place': '',
				'credit': Math.round(tables[i].rows[5].cells[1].innerText*10)/10 + ' credit(s), ' + tables[i].rows[4].cells[1].innerText,
			};
		}
	}
	for (var i = 0; i < classes.length; ++i)
	{
		console.log('HERPDERP' +
				classes[i].name +
					'DERPHERP' +
				'alert(\"' + [classes[i].section, classes[i].prof,
							  classes[i].time,
							  classes[i].place.replace(/Science Center/g, 'Sci'),
							  classes[i].credit].join('\\n') + '")'
		);
	}
}