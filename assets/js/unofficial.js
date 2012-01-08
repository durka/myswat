function ()
{
	var rows = document.getElementsByClassName('default2')[0].rows,
	
		semesters = [],
		cursem = null;
	
	for (var i = 0; i < rows.length; ++i) /* a row may be a semester colspan, a header row or a data row */
	{
		if (rows[i].cells.length == 1)
		{ /* it's a semester colspan */
			if (cursem != null) semesters[semesters.length] = cursem;
			var text = rows[i].cells[0].innerText.split(/\s+/);
			cursem = {
				'term': text[1] + ' ' + text[2],
				'credits': parseInt(text[text.length-1]),
				'grades': [],
			};
		}
		else if (rows[i].onmouseover != null)
		{ /* data rows have mouseover events */
			function T(j) { return rows[i].cells[j].innerText; }
			
			cursem.grades[cursem.grades.length] = {
				'name':		T(1),
				'detail':	T(0),
				'prof':		T(5),
				'credits':	T(2),
				'grade':	T(3),
			};
		}
	}
	if (cursem != null) semesters[semesters.length] = cursem;
	
	for (var i = 0; i < semesters.length; ++i)
	{
		var s = [];
		for (var j = 0; j < semesters[i].grades.length; ++j)
		{
			s[s.length] = [semesters[i].grades[j].grade + ' in ' + semesters[i].grades[j].name,
			               semesters[i].grades[j].detail,
			               semesters[i].grades[j].prof,
			               semesters[i].grades[j].credits + ' credit(s)'].filter(function (a) { return a != '-'; }).join('\\n\\t\\t');
		}
		console.log('HERPDERP' +
				semesters[i].term + ' (' + semesters[i].credits + ' credits)' +
					'DERPHERP' +
				'alert("' + s.join('\\n') + '")'
		);
	}
}