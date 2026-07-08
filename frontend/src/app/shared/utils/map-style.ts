import mapboxgl from 'mapbox-gl';

export function selectedMapStyle(
  mapboxPublicToken?: string,
): mapboxgl.StyleSpecification | string {
  const theme = (localStorage.getItem('saferoute_map_theme') || 'LIGHT').toUpperCase();
  const token = mapboxPublicToken?.trim();

  if (token) {
    switch (theme) {
      case 'DARK':
        return 'mapbox://styles/mapbox/dark-v11';
      case 'SATELLITE':
        return 'mapbox://styles/mapbox/satellite-streets-v12';
      default:
        return 'mapbox://styles/mapbox/streets-v12';
    }
  }

  const tiles =
    theme === 'DARK'
      ? ['https://a.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}.png']
      : theme === 'SATELLITE'
        ? [
            'https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}',
          ]
        : ['https://a.tile.openstreetmap.org/{z}/{x}/{y}.png'];

  return {
    version: 8,
    sources: {
      base: {
        type: 'raster',
        tiles,
        tileSize: 256,
      },
    },
    layers: [{ id: 'base', type: 'raster', source: 'base' }],
  };
}
