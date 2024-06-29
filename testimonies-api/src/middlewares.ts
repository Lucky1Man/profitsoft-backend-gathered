import { Request, Response, NextFunction } from "express";

export const dateInputMiddleware = (
  req: Request,
  _: Response,
  next: NextFunction
) => {
  const dateReviver = (_: string, value: unknown) => {
    if (value && typeof value === "string") {
      const dateRegex =
        /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d+)?(?:Z|[+-]\d{2}:\d{2})$/;
      if (dateRegex.test(value)) {
        return new Date(value);
      }
    }
    return value;
  };

  req.body = JSON.parse(JSON.stringify(req.body), dateReviver);
  next();
};
