export type CoordinatePair = [number, number];

export interface Bounds {
  minX: number;
  maxX: number;
  minY: number;
  maxY: number;
}

interface ViewportOptions {
  width?: number;
  height?: number;
  padding?: number;
}

export function createBounds(groups: CoordinatePair[][]): Bounds | null {
  const points = groups.flatMap((group) => group).filter(([x, y]) => Number.isFinite(x) && Number.isFinite(y));

  if (points.length === 0) {
    return null;
  }

  const xs = points.map(([x]) => x);
  const ys = points.map(([, y]) => y);

  const minX = Math.min(...xs);
  const maxX = Math.max(...xs);
  const minY = Math.min(...ys);
  const maxY = Math.max(...ys);

  return {
    minX,
    maxX: minX === maxX ? maxX + 0.001 : maxX,
    minY,
    maxY: minY === maxY ? maxY + 0.001 : maxY,
  };
}

export function toSvgPoints(
  points: CoordinatePair[],
  bounds: Bounds,
  options?: ViewportOptions,
): string {
  return points.map((point) => toSvgPoint(point, bounds, options)).join(' ');
}

export function toSvgPoint(
  point: CoordinatePair,
  bounds: Bounds,
  options?: ViewportOptions,
): string {
  const width = options?.width ?? 100;
  const height = options?.height ?? 100;
  const padding = options?.padding ?? 8;

  const normalizedX =
    padding +
    ((point[0] - bounds.minX) / (bounds.maxX - bounds.minX)) * (width - padding * 2);
  const normalizedY =
    height -
    padding -
    ((point[1] - bounds.minY) / (bounds.maxY - bounds.minY)) * (height - padding * 2);

  return `${normalizedX.toFixed(2)},${normalizedY.toFixed(2)}`;
}
