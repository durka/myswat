function ()
{
	/* feed in the table rows, which are in a <tbody> in the <table class="menuplaintable"> */
	var rows = document.getElementsByClassName('menuplaintable')[0].childNodes[1].childNodes;
	
	for (var i = 0; i < rows.length; ++i) /* these are rows of the table holding the menu entries */
	{
		if (rows[i].nodeName == 'TR') /* is this a row? */
		{
			var tds = rows[i].childNodes;
			for (var j = 0; j < tds.length; ++j) /* go through the row cells */
			{
				if (tds[j].childNodes.length > 1) /* is this a non-blank cell? */
				{ /* okay, grab the link info! */
					/* we use console.log with a special prefix so that our custom WebChromeClient will catch it */
					console.log('HERPDERP' +
						tds[j].childNodes[1].innerText +
								'DERPHERP' +
						'window.location="' + tds[j].childNodes[1].href + '"'
					);
				}
			}
		}
	}
}