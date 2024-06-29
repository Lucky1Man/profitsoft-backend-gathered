export class TestimonyFilterDto {
  executionFactId: string;
  size: number;
  from: number;

  constructor(data: Required<TestimonyFilterDto>) {
    this.executionFactId = data.executionFactId;
    this.size = data.size;
    this.from = data.from;
  }
}
