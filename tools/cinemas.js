const url = "https://www.cinema-city.pl/pl/data-api-service/v1/quickbook/10103/cinemas/with-event/until/2027-01-31?attr=&lang=pl_PL";

async function getCinemaIds(url) {
	const response = await fetch(url);

	if (!response.ok) {
		throw new Error("Błąd HTTP: " + response.status);
	}

	const data = await response.json();

	if (!data?.body?.cinemas || !Array.isArray(data.body.cinemas)) {
		throw new Error("Niepoprawna struktura danych");
	}

	return data.body.cinemas
		.map(cinema => cinema.id)
		.filter(id => id != null)
		.join(",");
}

getCinemaIds(url)
	.then(ids => {
		console.log(ids);
	})
	.catch(err => {
		console.error(err.message);
	});
