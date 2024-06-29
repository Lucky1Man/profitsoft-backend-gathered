import getConsulValue from "src/consul";

// Potential performance bottleneck 'await getConsulValue('execution-facts-service-address')'
// but I left it as think it is cool to be able to change service addresses without restarting app.
// Also it is not 100% a bottleneck as it needs measurements, if as a result of the measurements we will find
// out it causes significant performance decrease we wil just add caching here :) .

export const getExecutionFactById = async (
  id: string
): Promise<{ id: string }> => {
  const result = await (
    await fetch(
      `${await getConsulValue("execution-facts-service-address")}/${id}`,
      {
        method: "GET",
      }
    )
  ).json();
  // I receive a big object here as response but I decided to use only this value.
  // Also, here I separate node.js service structure from other(Java) service structure.
  return { id: result.id };
};
